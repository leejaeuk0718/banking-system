package com.jaeuk.job_ai.dto.ChatDto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateConversationTitleRequest {

    @NotBlank(message = "제목은 비워둘 수 없습니다")
    @Size(max = 200, message = "제목은 200자 이내로 입력해주세요")
    private String title;
}
