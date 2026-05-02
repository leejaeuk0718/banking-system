package com.jaeuk.job_ai.controller;

import com.jaeuk.job_ai.dto.ChatDto.ChatRequest;
import com.jaeuk.job_ai.dto.ChatDto.ChatResponse;
import com.jaeuk.job_ai.security.CustomUserDetails;
import com.jaeuk.job_ai.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Chat", description = "메시지 전송 (RAG)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    @Operation(
            summary = "메시지 전송",
            description = """
                    conversationId 가 비어 있으면 새 대화를 시작하고, 채워져 있으면
                    본인 소유의 기존 대화에 이어 붙인다. 응답에 어떤 conversationId 에 속했는지가 함께 반환된다.
                    """
    )
    public ResponseEntity<ChatResponse> chat(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ChatRequest request) {
        ChatResponse response = chatService.chat(userDetails.getUser(), request);
        return ResponseEntity.ok(response);
    }
}
