package com.jaeuk.job_ai.service;

import com.jaeuk.job_ai.dto.FeedbackDto.FeedbackRequest;
import com.jaeuk.job_ai.dto.FeedbackDto.FeedbackResponse;
import com.jaeuk.job_ai.entity.Feedback;
import com.jaeuk.job_ai.entity.Message;
import com.jaeuk.job_ai.entity.User;
import com.jaeuk.job_ai.repository.FeedbackRepository;
import com.jaeuk.job_ai.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final MessageRepository messageRepository;

    /**
     * 피드백 생성/갱신 (upsert).
     * 같은 (user, message) 쌍으로 이미 피드백이 있으면 type/comment 만 덮어쓴다.
     */
    @Transactional
    public FeedbackResponse upsert(User user, FeedbackRequest request) {
        Message message = messageRepository.findById(request.getMessageId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "메시지를 찾을 수 없습니다: " + request.getMessageId()));

        Feedback feedback = feedbackRepository.findByUserAndMessage(user, message)
                .map(existing -> {
                    existing.update(request.getType(), request.getComment());
                    return existing;
                })
                .orElseGet(() -> feedbackRepository.save(
                        Feedback.of(user, message, request.getType(), request.getComment())));

        return FeedbackResponse.from(feedback);
    }

    /** 피드백 삭제 (본인 것만). */
    @Transactional
    public void delete(User user, Long feedbackId) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "피드백을 찾을 수 없습니다: " + feedbackId));
        if (!feedback.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("피드백을 찾을 수 없습니다: " + feedbackId);
        }
        feedbackRepository.delete(feedback);
    }
}
