package com.jaeuk.job_ai.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 의료 문서 적재(Ingestion) 처리 상태.
 *
 * <pre>
 *  PROCESSING ──┬──► READY     (청킹 + 임베딩 + pgvector 적재 모두 성공)
 *               └──► FAILED    (어느 단계든 실패 — errorMessage 에 사유 기록)
 * </pre>
 */
@Getter
@RequiredArgsConstructor
public enum DocumentStatus {
    PROCESSING("처리 중"),
    READY("적재 완료"),
    FAILED("실패");

    private final String displayName;
}
