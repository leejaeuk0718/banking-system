package com.jaeuk.job_ai.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * 한 {@link Document} 가 청킹된 조각의 메타데이터.
 *
 * <p>{@link #vectorId} 는 Spring AI {@code VectorStore} 가 발급한 UUID 로,
 * pgvector 테이블의 행과 1:1 로 매핑된다. RDB 측에선 {@code DocumentChunk} 로
 * "어느 문서의 몇 번째 청크가 어느 벡터인가"를 추적하고, 유사도 검색 자체는 pgvector 가 처리한다.</p>
 */
@Entity
@Table(name = "document_chunks",
        indexes = {
                @Index(name = "idx_chunk_doc_index",  columnList = "document_id, chunkIndex"),
                @Index(name = "idx_chunk_vector_id", columnList = "vectorId", unique = true)
        })
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentChunk extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)   // Document 삭제 시 청크 메타도 함께 제거
    private Document document;

    /** 원본 문서 내에서의 청크 순번 (0-based). 출처 표시 정렬에 사용. */
    @Column(nullable = false)
    private int chunkIndex;

    /** 청크 본문 미리보기 — UI 의 출처 칩에 짧게 노출하는 용도(전체 본문은 pgvector 에 있다). */
    @Column(nullable = false, length = 500)
    private String contentPreview;

    /** pgvector 행 PK 와 동일한 UUID 문자열. */
    @Column(nullable = false, length = 64)
    private String vectorId;

    public static DocumentChunk of(Document document, int chunkIndex, String content, String vectorId) {
        String preview = (content == null) ? "" :
                content.length() <= 500 ? content : content.substring(0, 500);
        return DocumentChunk.builder()
                .document(document)
                .chunkIndex(chunkIndex)
                .contentPreview(preview)
                .vectorId(vectorId)
                .build();
    }
}
