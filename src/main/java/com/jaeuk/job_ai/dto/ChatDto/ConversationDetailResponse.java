package com.jaeuk.job_ai.dto.ChatDto;

import com.jaeuk.job_ai.entity.Conversation;
import com.jaeuk.job_ai.entity.Message;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 대화 상세 화면 — 메시지 전체를 시간순으로 포함.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDetailResponse {

    private Long id;
    private String title;
    private LocalDateTime createdAt;
    private LocalDateTime lastMessageAt;
    private List<MessageResponse> messages;

    public static ConversationDetailResponse of(Conversation c, List<Message> messages) {
        return ConversationDetailResponse.builder()
                .id(c.getId())
                .title(c.getTitle())
                .createdAt(c.getCreatedAt())
                .lastMessageAt(c.getLastMessageAt())
                .messages(messages.stream().map(MessageResponse::from).toList())
                .build();
    }
}
