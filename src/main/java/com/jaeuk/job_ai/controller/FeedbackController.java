package com.jaeuk.job_ai.controller;

import com.jaeuk.job_ai.dto.FeedbackDto.FeedbackRequest;
import com.jaeuk.job_ai.dto.FeedbackDto.FeedbackResponse;
import com.jaeuk.job_ai.security.CustomUserDetails;
import com.jaeuk.job_ai.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Feedback", description = "AI 답변에 좋아요 / 싫어요 피드백")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/feedbacks")
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    @Operation(
            summary = "피드백 생성/갱신 (upsert)",
            description = "같은 메시지에 이미 피드백이 있으면 덮어씁니다.")
    public ResponseEntity<FeedbackResponse> upsert(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody FeedbackRequest request) {
        return ResponseEntity.ok(feedbackService.upsert(userDetails.getUser(), request));
    }

    @DeleteMapping("/{feedbackId}")
    @Operation(summary = "피드백 삭제")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long feedbackId) {
        feedbackService.delete(userDetails.getUser(), feedbackId);
        return ResponseEntity.noContent().build();
    }
}
