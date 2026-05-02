package com.jaeuk.job_ai.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * LLM 호출 1건의 사용량 로그.
 *
 * <p>토큰 수·지연시간을 기록해 과금 분석, 응답 품질 모니터링, RAG 성능 추적에 활용한다.
 * 비동기(@Async)로 적재되므로 채팅 응답 지연에 영향을 주지 않는다.</p>
 */
@Entity
@Table(name = "usage_logs",
        indexes = {
                @Index(name = "idx_usage_user_created",  columnList = "user_id, createdAt DESC"),
                @Index(name = "idx_usage_message",       columnList = "message_id")
        })
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsageLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    /** 이 로그가 대응하는 ASSISTANT 메시지. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Message message;

    /** 프롬프트 + 컨텍스트 토큰 수 (입력). */
    @Column(nullable = false)
    private int promptTokens;

    /** 생성된 답변 토큰 수 (출력). */
    @Column(nullable = false)
    private int completionTokens;

    /** LLM 응답까지 걸린 시간 (밀리초). */
    @Column(nullable = false)
    private long latencyMs;

    /** RAG 검색에서 반환된 문서 청크 수. */
    @Column(nullable = false)
    private int retrievedChunks;

    public static UsageLog of(User user, Message message,
                              int promptTokens, int completionTokens,
                              long latencyMs, int retrievedChunks) {
        return UsageLog.builder()
                .user(user)
                .message(message)
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .latencyMs(latencyMs)
                .retrievedChunks(retrievedChunks)
                .build();
    }
}
