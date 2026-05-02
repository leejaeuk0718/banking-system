package com.jaeuk.job_ai.controller;

import com.jaeuk.job_ai.dto.ChatDto.ConversationDetailResponse;
import com.jaeuk.job_ai.dto.ChatDto.ConversationSummaryResponse;
import com.jaeuk.job_ai.dto.ChatDto.UpdateConversationTitleRequest;
import com.jaeuk.job_ai.security.CustomUserDetails;
import com.jaeuk.job_ai.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Conversation", description = "대화 세션 관리 (목록/상세/이름변경/삭제)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ChatService chatService;

    @GetMapping
    @Operation(summary = "내 대화 목록", description = "사이드바 '최근 대화'에 표시되는 목록 — 마지막 메시지 시각 내림차순")
    public ResponseEntity<List<ConversationSummaryResponse>> listConversations(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(chatService.listConversations(userDetails.getUser()));
    }

    @GetMapping("/{conversationId}")
    @Operation(summary = "대화 상세", description = "메시지 전체를 시간순으로 반환")
    public ResponseEntity<ConversationDetailResponse> getConversation(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long conversationId) {
        return ResponseEntity.ok(chatService.getConversation(userDetails.getUser(), conversationId));
    }

    @PatchMapping("/{conversationId}")
    @Operation(summary = "대화 이름 변경")
    public ResponseEntity<Void> renameConversation(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long conversationId,
            @Valid @RequestBody UpdateConversationTitleRequest request) {
        chatService.renameConversation(userDetails.getUser(), conversationId, request.getTitle());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{conversationId}")
    @Operation(summary = "대화 삭제", description = "포함된 메시지도 함께 제거된다 (FK CASCADE)")
    public ResponseEntity<Void> deleteConversation(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long conversationId) {
        chatService.deleteConversation(userDetails.getUser(), conversationId);
        return ResponseEntity.noContent().build();
    }
}
