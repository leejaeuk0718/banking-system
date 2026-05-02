package com.jaeuk.job_ai.entity;

import com.jaeuk.job_ai.enums.FeedbackType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * 사용자가 ASSISTANT 메시지에 남기는 좋아요(LIKE) / 싫어요(DISLIKE) 피드백.
 *
 * <ul>
 *   <li>한 사용자가 한 메시지에 하나의 피드백만 남길 수 있다 (unique 제약).</li>
 *   <li>{@code comment} 는 선택 항목으로, 추가 의견을 자유 텍스트로 기록한다.</li>
 * </ul>
 */
@Entity
@Table(name = "feedbacks",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_feedback_user_message",
                columnNames = {"user_id", "message_id"}),
        indexes = @Index(name = "idx_feedback_message", columnList = "message_id"))
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Feedback extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Message message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private FeedbackType type;

    /** 선택 의견 (최대 500자). */
    @Column(length = 500)
    private String comment;

    public static Feedback of(User user, Message message, FeedbackType type, String comment) {
        return Feedback.builder()
                .user(user)
                .message(message)
                .type(type)
                .comment(comment)
                .build();
    }

    /** 피드백 유형/코멘트 갱신 (같은 메시지에 재투표 시 사용). */
    public void update(FeedbackType type, String comment) {
        this.type = type;
        this.comment = comment;
    }
}
