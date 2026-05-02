package com.jaeuk.job_ai.dto.ChatDto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 챗 메시지 전송 요청.
 * - {@code conversationId} 가 {@code null} 이면 새 대화를 시작한다.
 * - 채워져 있으면 본인 소유의 기존 대화에 이어 붙는다(다른 사용자 소유면 404).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    private Long conversationId;

    @NotBlank(message = "메시지를 입력해주세요")
    private String message;
}
