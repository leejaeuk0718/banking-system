package com.jaeuk.job_ai.service;

import com.jaeuk.job_ai.dto.DocumentDto.DocumentDetailResponse;
import com.jaeuk.job_ai.dto.DocumentDto.DocumentResponse;
import com.jaeuk.job_ai.entity.Document;
import com.jaeuk.job_ai.entity.DocumentChunk;
import com.jaeuk.job_ai.entity.User;
import com.jaeuk.job_ai.exception.DocumentNotFoundException;
import com.jaeuk.job_ai.repository.DocumentChunkRepository;
import com.jaeuk.job_ai.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 의료 문서 적재(Ingestion) 파이프라인.
 *
 * <pre>
 *   업로드(MultipartFile)
 *      │
 *      ▼
 *   [1] Document 메타 저장 (status = PROCESSING)  ← 동기, 즉시 반환
 *      │
 *      ▼   ── 이하 @Async 비동기 실행 ──────────────────────────────────
 *   [2] DocumentReader 로 텍스트 추출 (PDF → PagePdfDocumentReader, 그 외 → TikaDocumentReader)
 *      │
 *      ▼
 *   [3] TokenTextSplitter 로 청킹 (기본 ~800 토큰, ~350 오버랩)
 *      │
 *      ▼
 *   [4] 각 청크의 메타데이터에 documentId/chunkIndex/source/filename 주입
 *      │
 *      ▼
 *   [5] vectorStore.add(chunks) — 임베딩 호출 + pgvector 저장 (한 번의 일괄 호출)
 *      │
 *      ▼
 *   [6] 청크별 DocumentChunk 행을 RDB 에 저장 (vectorId = Spring AI 가 발급한 UUID)
 *      │
 *      ▼
 *   [7] Document.markReady(chunkCount)
 * </pre>
 *
 * <p>각 단계마다 {@link IngestProgressService} 를 통해 SSE 로 진행률을 푸시한다.
 * 어느 단계든 실패하면 {@link Document#markFailed(String)} 으로 상태 기록 후 SSE 에 오류 이벤트를 발행한다.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IngestService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final VectorStore vectorStore;
    private final IngestProgressService progressService;

    /**
     * 1단계(동기): Document 메타 행을 PROCESSING 상태로 즉시 저장하고 반환.
     * 클라이언트는 반환된 documentId 로 SSE 구독 후 업로드 진행 상황을 추적한다.
     */
    @Transactional
    public DocumentResponse startIngest(MultipartFile file, String source, User uploader) {
        Document document = documentRepository.save(Document.start(
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                source,
                uploader
        ));
        log.info("Ingest started: id={}, file={}", document.getId(), file.getOriginalFilename());
        return DocumentResponse.from(document);
    }

    /**
     * 2~7단계(비동기): 실제 파싱/임베딩/저장 작업을 백그라운드 스레드에서 실행.
     *
     * <p>{@code startIngest()} 의 반환 후 컨트롤러가 호출한다.
     * 진행률은 SSE 이벤트({@link IngestProgressService})로 클라이언트에 전달된다.</p>
     *
     * @param documentId {@code startIngest} 에서 만들어진 Document ID
     * @param bytes      파일 바이트 (MultipartFile 은 스레드 경계를 넘으면 닫힐 수 있으므로 미리 읽어 전달)
     * @param filename   원본 파일명
     * @param contentType MIME 타입
     * @param source     출처명
     */
    @Async("taskExecutor")
    @Transactional
    public void runIngestAsync(Long documentId, byte[] bytes,
                               String filename, String contentType, String source) {

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalStateException("Document not found: " + documentId));

        try {
            Resource resource = new ByteArrayResource(bytes) {
                @Override public String getFilename() { return filename; }
            };

            // [2] 텍스트 추출 — 10%
            progressService.send(documentId, "텍스트 추출 중", 10);
            List<org.springframework.ai.document.Document> rawDocs =
                    "application/pdf".equalsIgnoreCase(contentType)
                            ? new PagePdfDocumentReader(resource).get()
                            : new TikaDocumentReader(resource).get();

            // [3] 청킹 — 30%
            progressService.send(documentId, "청킹 중", 30);
            List<org.springframework.ai.document.Document> chunks =
                    new TokenTextSplitter().apply(rawDocs);

            // [4] 메타데이터 보강 — 40%
            progressService.send(documentId, "메타데이터 주입 중", 40);
            int idx = 0;
            for (org.springframework.ai.document.Document chunk : chunks) {
                chunk.getMetadata().put("documentId", documentId);
                chunk.getMetadata().put("chunkIndex", idx);
                chunk.getMetadata().put("source",     source);
                chunk.getMetadata().put("filename",   filename);
                idx++;
            }

            // [5] 임베딩 + 벡터 저장 — 60%
            progressService.send(documentId, "임베딩 생성 및 벡터 저장 중", 60);
            vectorStore.add(chunks);

            // [6] DocumentChunk 영속화 — 80%
            progressService.send(documentId, "청크 메타 저장 중", 80);
            int chunkIndex = 0;
            for (org.springframework.ai.document.Document chunk : chunks) {
                documentChunkRepository.save(DocumentChunk.of(
                        document, chunkIndex++, chunk.getText(), chunk.getId()));
            }

            // [7] 완료 — 100%
            document.markReady(chunks.size());
            log.info("Document ingested: id={}, chunks={}", documentId, chunks.size());
            progressService.complete(documentId);

        } catch (Exception e) {
            log.error("Document ingestion failed: id={}", documentId, e);
            document.markFailed(e.getClass().getSimpleName() + ": " + e.getMessage());
            progressService.completeWithError(documentId, "적재 실패: " + e.getMessage());
        }
    }

    /**
     * 하위 호환 동기 메서드 — 기존 코드/테스트에서 사용.
     * 내부적으로 startIngest + runIngestAsync 를 순차 호출한다.
     *
     * @deprecated 컨트롤러에서 직접 startIngest + runIngestAsync 를 호출하는 방식을 권장.
     */
    @Deprecated
    public DocumentResponse ingest(MultipartFile file, String source, User uploader) {
        DocumentResponse resp = startIngest(file, source, uploader);
        byte[] bytes;
        try { bytes = file.getBytes(); }
        catch (IOException e) { throw new RuntimeException("파일 읽기 실패", e); }
        runIngestAsync(resp.getId(), bytes, file.getOriginalFilename(),
                file.getContentType(), source);
        return resp;
    }

    /**
     * 문서 삭제 — pgvector 의 벡터 행 + RDB 의 청크/문서 레코드 모두 정리.
     */
    @Transactional
    public void delete(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));

        List<DocumentChunk> chunks = documentChunkRepository.findByDocument(document);
        List<String> vectorIds = chunks.stream().map(DocumentChunk::getVectorId).toList();

        // pgvector 에서 임베딩 행 제거 (RDB CASCADE 와 별개로 명시적으로 호출해야 함)
        if (!vectorIds.isEmpty()) {
            vectorStore.delete(vectorIds);
        }

        // DocumentChunk 는 FK ON DELETE CASCADE 로 함께 제거된다
        documentRepository.delete(document);
        log.info("Document deleted: id={}, vectors={}", documentId, vectorIds.size());
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> listAll() {
        return documentRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(DocumentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public DocumentDetailResponse getDetail(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
        List<DocumentChunk> chunks =
                documentChunkRepository.findByDocumentOrderByChunkIndexAsc(document);
        return DocumentDetailResponse.of(document, chunks);
    }

    // ───────────────────────── 내부 ─────────────────────────

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("파일 읽기 실패: " + file.getOriginalFilename(), e);
        }
    }
}
