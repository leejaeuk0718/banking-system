package com.jaeuk.job_ai.entity;

import com.jaeuk.job_ai.enums.MessageRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * 대화 내 개별 메시지. 사용자(USER) 와 어시스턴트(ASSISTANT) 발화가 모두 같은 테이블에 누적된다.
 * 출처(citation) 는 추후 {@code MessageSource} 또는 JSON 컬럼으로 확장 예정.
 */
@Entity
@Table(name = "messages",
        indexes = @Index(name = "idx_msg_conv_created", columnList = "conversation_id, createdAt"))
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)   // Conversation 삭제 시 메시지도 같이 제거
    private Conversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MessageRole role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    public static Message of(Conversation conversation, MessageRole role, String content) {
        return Message.builder()
                .conversation(conversation)
                .role(role)
                .content(content)
                .build();
    }
}
