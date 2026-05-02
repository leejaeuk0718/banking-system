package com.jaeuk.job_ai.dto.FeedbackDto;

import com.jaeuk.job_ai.entity.Feedback;
import com.jaeuk.job_ai.enums.FeedbackType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class FeedbackResponse {

    private Long id;
    private Long messageId;
    private FeedbackType type;
    private String comment;
    private LocalDateTime createdAt;

    public static FeedbackResponse from(Feedback feedback) {
        return FeedbackResponse.builder()
                .id(feedback.getId())
                .messageId(feedback.getMessage().getId())
                .type(feedback.getType())
                .comment(feedback.getComment())
                .createdAt(feedback.getCreatedAt())
                .build();
    }
}
