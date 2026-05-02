package com.jaeuk.job_ai.entity;

import com.jaeuk.job_ai.enums.DocumentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * 관리자가 업로드한 의료 문서 한 건의 메타데이터.
 *
 * 원본 파일 자체는 보존하지 않는다(저장소 비용 / GDPR / 의료정보 보호 관점).
 * 적재 후에는 {@link DocumentChunk} + pgvector 의 임베딩 행만 남고, 재적재가 필요하면 다시 업로드한다.
 */
@Entity
@Table(name = "documents",
        indexes = @Index(name = "idx_doc_status_created", columnList = "status, createdAt DESC"))
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String originalFilename;

    @Column(length = 100)
    private String contentType;

    @Column(nullable = false)
    private long sizeBytes;

    /** 사용자에게 보여줄 출처명. 예: "2024 대한고혈압학회 진료지침" */
    @Column(nullable = false, length = 200)
    private String source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User uploader;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DocumentStatus status;

    /** 적재 완료된 청크 개수. {@link DocumentStatus#READY} 일 때만 유효. */
    @Column(nullable = false)
    private int chunkCount;

    /** 실패 시 사유 (스택트레이스 요약). FAILED 가 아니면 null. */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    public static Document start(String originalFilename, String contentType, long sizeBytes,
                                 String source, User uploader) {
        return Document.builder()
                .originalFilename(originalFilename)
                .contentType(contentType)
                .sizeBytes(sizeBytes)
                .source(source)
                .uploader(uploader)
                .status(DocumentStatus.PROCESSING)
                .chunkCount(0)
                .build();
    }

    public void markReady(int chunkCount) {
        this.status = DocumentStatus.READY;
        this.chunkCount = chunkCount;
        this.errorMessage = null;
    }

    public void markFailed(String message) {
        this.status = DocumentStatus.FAILED;
        // 에러 메시지가 너무 길면 잘라서 저장 (DB TEXT 컬럼이지만 실용적 한도)
        this.errorMessage = (message != null && message.length() > 2000)
                ? message.substring(0, 2000)
                : message;
    }
}
