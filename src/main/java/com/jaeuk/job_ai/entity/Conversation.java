package com.jaeuk.job_ai.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/**
 * 대화 세션. 한 사용자에 다수의 대화가 존재하며, 각 대화는 시간순 {@link Message} 들을 가진다.
 * 사이드바의 "최근 대화" 목록은 {@link #lastMessageAt} 내림차순으로 그린다.
 */
@Entity
@Table(name = "conversations",
        indexes = @Index(name = "idx_conv_user_last_msg", columnList = "user_id, lastMessageAt DESC"))
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)   // User 삭제 시 대화도 같이 제거
    private User user;

    @Column(nullable = false, length = 200)
    private String title;

    /**
     * 마지막 메시지 시각. {@code BaseEntity.updatedAt} 은 엔티티 자체 수정 시에만 갱신되므로
     * "메시지 추가"라는 자식 이벤트를 별도 컬럼으로 잡는다(목록 정렬 인덱스의 키).
     */
    @Column(nullable = false)
    private LocalDateTime lastMessageAt;

    public static Conversation start(User user, String title) {
        return Conversation.builder()
                .user(user)
                .title(title)
                .lastMessageAt(LocalDateTime.now())
                .build();
    }

    public void rename(String newTitle) {
        this.title = newTitle;
    }

    public void touchLastMessageAt() {
        this.lastMessageAt = LocalDateTime.now();
    }

    public boolean isOwnedBy(User other) {
        return this.user != null && this.user.getId().equals(other.getId());
    }
}
