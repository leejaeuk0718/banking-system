package com.jaeuk.job_ai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 문서 적재(Ingest) 진행률을 SSE 로 푸시하는 허브.
 *
 * <p>흐름:</p>
 * <pre>
 *   1. 클라이언트 → GET /api/admin/documents/{documentId}/progress  → subscribe()
 *   2. IngestService(Async) → send() 로 단계별 진행률 이벤트 발행
 *   3. 완료 / 실패 → complete() / completeWithError()
 * </pre>
 *
 * <p>서버 재시작 또는 스케일-아웃 환경에서는 각 인스턴스가 독립적인 emitter 맵을 가지므로,
 * 클러스터 배포 시 Redis Pub/Sub 기반 구현으로 교체해야 한다.</p>
 */
@Service
@Slf4j
public class IngestProgressService {

    /** documentId → SseEmitter */
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    /** SSE 타임아웃 (ms). 긴 문서도 여유 있게 처리할 수 있도록 5분 설정. */
    private static final long SSE_TIMEOUT_MS = 5 * 60 * 1000L;

    /**
     * 클라이언트가 특정 documentId 의 진행률을 구독할 때 호출.
     * 기존 emitter 가 있으면 먼저 닫는다.
     */
    public SseEmitter subscribe(Long documentId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        emitter.onCompletion(() -> emitters.remove(documentId));
        emitter.onTimeout(() -> {
            log.warn("SSE timeout: documentId={}", documentId);
            emitters.remove(documentId);
        });
        emitter.onError(e -> {
            log.warn("SSE error: documentId={}, error={}", documentId, e.getMessage());
            emitters.remove(documentId);
        });

        emitters.put(documentId, emitter);
        return emitter;
    }

    /**
     * 진행률 이벤트 발행 ({@code progress} 이벤트명).
     *
     * @param documentId 문서 ID
     * @param step       단계 설명 (예: "텍스트 추출 중", "청킹", "임베딩 저장")
     * @param percent    0–100
     */
    public void send(Long documentId, String step, int percent) {
        SseEmitter emitter = emitters.get(documentId);
        if (emitter == null) return;
        try {
            emitter.send(SseEmitter.event()
                    .name("progress")
                    .data(new ProgressEvent(step, percent)));
        } catch (IOException e) {
            log.warn("SSE send failed: documentId={}", documentId);
            emitters.remove(documentId);
        }
    }

    /** 적재 완료 — emitter 정상 종료. */
    public void complete(Long documentId) {
        SseEmitter emitter = emitters.remove(documentId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("done")
                        .data(new ProgressEvent("완료", 100)));
            } catch (IOException ignored) {}
            emitter.complete();
        }
    }

    /** 적재 실패 — emitter 에 에러 전달 후 종료. */
    public void completeWithError(Long documentId, String errorMessage) {
        SseEmitter emitter = emitters.remove(documentId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data(new ProgressEvent(errorMessage, -1)));
            } catch (IOException ignored) {}
            emitter.completeWithError(new RuntimeException(errorMessage));
        }
    }

    // ─── 내부 이벤트 페이로드 ────────────────────────────────────

    public record ProgressEvent(String step, int percent) {}
}
