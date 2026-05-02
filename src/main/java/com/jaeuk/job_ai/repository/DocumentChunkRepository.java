package com.jaeuk.job_ai.repository;

import com.jaeuk.job_ai.entity.Document;
import com.jaeuk.job_ai.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    List<DocumentChunk> findByDocumentOrderByChunkIndexAsc(Document document);

    /** 문서 삭제 시 pgvector 에 보낼 vectorId 목록 수집용 */
    List<DocumentChunk> findByDocument(Document document);

    /** 출처 칩 렌더링 시 vectorId → chunk 메타 역조회 */
    List<DocumentChunk> findByVectorIdIn(List<String> vectorIds);
}
