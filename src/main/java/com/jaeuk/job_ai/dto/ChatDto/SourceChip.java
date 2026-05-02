package com.jaeuk.job_ai.dto.ChatDto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.ai.document.Document;

/**
 * RAG 검색 결과로 참조된 문서 조각(청크)의 출처 정보.
 *
 * <p>프론트엔드는 이 목록을 답변 아래 "출처 칩"으로 렌더링한다.</p>
 */
@Getter
@Builder
public class SourceChip {

    /** 사람이 읽을 수 있는 문서 출처명. 예: "2024 대한고혈압학회 진료지침" */
    private String source;

    /** 원본 파일명. 예: "hypertension_guideline_2024.pdf" */
    private String filename;

    /** 문서 내 청크 인덱스 (0-based). */
    private Integer chunkIndex;

    /** pgvector 코사인 유사도 점수 (0.0 ~ 1.0). 높을수록 관련성 높음. */
    private Double score;

    /**
     * Spring AI {@link Document} 의 메타데이터에서 SourceChip 을 생성한다.
     *
     * <p>메타데이터 키는 {@link com.jaeuk.job_ai.service.IngestService} 에서 주입한 것과 동일해야 한다.
     * ({@code source}, {@code filename}, {@code chunkIndex})</p>
     */
    public static SourceChip from(Document doc) {
        var meta = doc.getMetadata();
        return SourceChip.builder()
                .source(asString(meta.get("source")))
                .filename(asString(meta.get("filename")))
                .chunkIndex(asInt(meta.get("chunkIndex")))
                .score(asDouble(doc.getScore()))
                .build();
    }

    private static String asString(Object o) {
        return o == null ? null : o.toString();
    }

    private static Integer asInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        try { return Integer.parseInt(o.toString()); } catch (NumberFormatException e) { return null; }
    }

    private static Double asDouble(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.doubleValue();
        return null;
    }
}
