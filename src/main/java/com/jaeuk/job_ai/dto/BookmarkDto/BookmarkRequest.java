package com.jaeuk.job_ai.dto.BookmarkDto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BookmarkRequest {

    @NotNull(message = "messageId 는 필수입니다")
    private Long messageId;

    @Size(max = 300, message = "메모는 300자 이하여야 합니다")
    private String note;
}
