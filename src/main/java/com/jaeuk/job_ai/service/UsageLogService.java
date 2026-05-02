package com.jaeuk.job_ai.service;

import com.jaeuk.job_ai.entity.Message;
import com.jaeuk.job_ai.entity.UsageLog;
import com.jaeuk.job_ai.entity.User;
import com.jaeuk.job_ai.repository.UsageLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * LLM 호출 사용량을 비동기로 기록한다.
 *
 * <p>{@code @Async("taskExecutor")} 로 채팅 응답 스레드와 분리되므로
 * 적재 실패가 사용자 응답에 영향을 주지 않는다.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UsageLogService {

    private final UsageLogRepository usageLogRepository;

    /**
     * 사용량 로그 비동기 저장.
     *
     * @param user             요청 사용자
     * @param assistantMessage 저장된 AI 응답 메시지
     * @param promptTokens     입력 토큰 수
     * @param completionTokens 출력 토큰 수
     * @param latencyMs        LLM 응답 지연(ms)
     * @param retrievedChunks  RAG 검색 청크 수
     */
    @Async("taskExecutor")
    @Transactional
    public void saveAsync(User user, Message assistantMessage,
                          int promptTokens, int completionTokens,
                          long latencyMs, int retrievedChunks) {
        try {
            usageLogRepository.save(UsageLog.of(
                    user, assistantMessage,
                    promptTokens, completionTokens,
                    latencyMs, retrievedChunks));
        } catch (Exception e) {
            // 사용량 로그 실패는 서비스 운영에 치명적이지 않으므로 경고 로그만 남김
            log.warn("UsageLog 저장 실패: messageId={}, error={}",
                    assistantMessage.getId(), e.getMessage());
        }
    }
}
