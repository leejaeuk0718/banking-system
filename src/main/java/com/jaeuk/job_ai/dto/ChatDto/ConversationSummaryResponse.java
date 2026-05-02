package com.jaeuk.job_ai.dto.ChatDto;

import com.jaeuk.job_ai.entity.Conversation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사이드바 "최근 대화" 목록 한 줄. 메시지는 포함하지 않는다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationSummaryResponse {

    private Long id;
    private String title;
    private LocalDateTime lastMessageAt;
    private LocalDateTime createdAt;

    public static ConversationSummaryResponse from(Conversation c) {
        return ConversationSummaryResponse.builder()
                .id(c.getId())
                .title(c.getTitle())
                .lastMessageAt(c.getLastMessageAt())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
