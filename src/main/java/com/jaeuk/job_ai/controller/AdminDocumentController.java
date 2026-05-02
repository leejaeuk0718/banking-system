package com.jaeuk.job_ai.controller;

import com.jaeuk.job_ai.dto.DocumentDto.DocumentDetailResponse;
import com.jaeuk.job_ai.dto.DocumentDto.DocumentResponse;
import com.jaeuk.job_ai.security.CustomUserDetails;
import com.jaeuk.job_ai.service.IngestProgressService;
import com.jaeuk.job_ai.service.IngestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

/**
 * 관리자 전용 — 의료 문서 적재/조회/삭제. URL 패턴 {@code /api/admin/**} 은 SecurityConfig 에서
 * {@code hasRole("ADMIN")} 으로 잠겨 있다.
 */
@Tag(name = "Admin/Documents", description = "[관리자] 의료 문서 적재(RAG 지식베이스) 관리")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/documents")
@Validated
public class AdminDocumentController {

    private final IngestService ingestService;
    private final IngestProgressService progressService;

    /**
     * 비동기 업로드.
     *
     * <ol>
     *   <li>파일 바이트를 읽고 Document 메타를 PROCESSING 상태로 즉시 저장한다 (동기).</li>
     *   <li>documentId 를 201 로 반환한다.</li>
     *   <li>클라이언트는 {@code GET /{documentId}/progress} 로 SSE 를 구독해 진행률을 수신한다.</li>
     *   <li>실제 파싱/임베딩은 백그라운드 스레드에서 실행된다 (비동기).</li>
     * </ol>
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "문서 업로드 (비동기)",
            description = """
                    파일 업로드 후 documentId 를 즉시 반환합니다.
                    실제 텍스트 추출 → 청킹 → 임베딩 → pgvector 저장은 백그라운드에서 비동기로 처리되며,
                    GET /{documentId}/progress (SSE) 로 진행률을 구독할 수 있습니다.
                    """
    )
    public ResponseEntity<DocumentResponse> upload(
            @AuthenticationPrincipal CustomUserDetails admin,
            @RequestPart("file") MultipartFile file,
            @RequestPart("source") @NotBlank @Size(max = 200) String source) throws IOException {

        // 파일 바이트를 미리 읽는다 — @Async 경계에서 MultipartFile 이 닫히는 것을 방지
        byte[] bytes = file.getBytes();

        DocumentResponse response = ingestService.startIngest(file, source, admin.getUser());

        // 백그라운드 적재 시작 (즉시 반환)
        ingestService.runIngestAsync(
                response.getId(), bytes,
                file.getOriginalFilename(), file.getContentType(), source);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * SSE 구독 — 특정 문서의 적재 진행률을 실시간으로 수신한다.
     *
     * <p>이벤트 종류:</p>
     * <ul>
     *   <li>{@code progress} — {@code {"step":"...", "percent": 0~99}} 형태 JSON</li>
     *   <li>{@code done}     — {@code {"step":"완료", "percent": 100}}</li>
     *   <li>{@code error}    — {@code {"step":"오류 메시지", "percent": -1}}</li>
     * </ul>
     */
    @GetMapping(value = "/{documentId}/progress", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "적재 진행률 SSE 구독",
            description = "documentId 별로 SSE 스트림을 열어 단계별 진행률 이벤트를 수신합니다."
    )
    public SseEmitter progressStream(@PathVariable Long documentId) {
        return progressService.subscribe(documentId);
    }

    @GetMapping
    @Operation(summary = "문서 목록", description = "최근 업로드 순")
    public ResponseEntity<List<DocumentResponse>> list() {
        return ResponseEntity.ok(ingestService.listAll());
    }

    @GetMapping("/{documentId}")
    @Operation(summary = "문서 상세", description = "포함된 청크 목록까지 함께 반환")
    public ResponseEntity<DocumentDetailResponse> detail(@PathVariable Long documentId) {
        return ResponseEntity.ok(ingestService.getDetail(documentId));
    }

    @DeleteMapping("/{documentId}")
    @Operation(summary = "문서 삭제", description = "pgvector 의 임베딩 행 + RDB 의 청크/문서 메타 모두 제거")
    public ResponseEntity<Void> delete(@PathVariable Long documentId) {
        ingestService.delete(documentId);
        return ResponseEntity.noContent().build();
    }
}
