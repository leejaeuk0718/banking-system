package com.jaeuk.job_ai.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * 사용자가 나중에 다시 볼 ASSISTANT 메시지를 저장하는 북마크.
 *
 * <ul>
 *   <li>한 사용자가 한 메시지에 하나의 북마크만 생성할 수 있다 (unique 제약).</li>
 *   <li>{@code note} 는 선택 항목 — 메모를 붙이고 싶을 때 사용.</li>
 * </ul>
 */
@Entity
@Table(name = "bookmarks",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_bookmark_user_message",
                columnNames = {"user_id", "message_id"}),
        indexes = @Index(name = "idx_bookmark_user_created", columnList = "user_id, createdAt DESC"))
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bookmark extends BaseEntity {

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

    /** 선택 메모 (최대 300자). */
    @Column(length = 300)
    private String note;

    public static Bookmark of(User user, Message message, String note) {
        return Bookmark.builder()
                .user(user)
                .message(message)
                .note(note)
                .build();
    }

    public void updateNote(String note) {
        this.note = note;
    }
}
