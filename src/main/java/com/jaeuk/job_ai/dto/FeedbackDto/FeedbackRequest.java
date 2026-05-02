package com.jaeuk.job_ai.dto.FeedbackDto;

import com.jaeuk.job_ai.enums.FeedbackType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FeedbackRequest {

    @NotNull(message = "messageId 는 필수입니다")
    private Long messageId;

    @NotNull(message = "type(LIKE/DISLIKE) 은 필수입니다")
    private FeedbackType type;

    @Size(max = 500, message = "코멘트는 500자 이하여야 합니다")
    private String comment;
}
