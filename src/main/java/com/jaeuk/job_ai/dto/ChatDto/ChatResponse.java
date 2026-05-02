package com.jaeuk.job_ai.dto.ChatDto;

import com.jaeuk.job_ai.entity.Conversation;
import com.jaeuk.job_ai.entity.Message;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 챗 메시지 전송 응답.
 *
 * <ul>
 *   <li>AI 답변 텍스트</li>
 *   <li>어떤 대화(conversationId)에 속했는지 — 신규 생성된 경우에도 반환</li>
 *   <li>RAG 검색 결과로 참조된 출처 칩 목록 ({@link SourceChip})</li>
 * </ul>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    private Long conversationId;
    private String conversationTitle;
    private Long messageId;
    private String content;
    private LocalDateTime createdAt;

    /**
     * RAG 검색에서 참조된 문서 청크 출처 목록.
     * 벡터 유사도가 threshold 를 넘는 항목만 포함된다.
     * 빈 리스트이면 문서 컨텍스트 없이 LLM 이 답변한 것이다.
     */
    private List<SourceChip> sources;

    public static ChatResponse from(Conversation conv, Message msg, List<SourceChip> sources) {
        return ChatResponse.builder()
                .conversationId(conv.getId())
                .conversationTitle(conv.getTitle())
                .messageId(msg.getId())
                .content(msg.getContent())
                .createdAt(msg.getCreatedAt())
                .sources(sources == null ? List.of() : sources)
                .build();
    }

    /** 하위 호환 팩토리 — sources 없이 호출할 경우용 (테스트 등). */
    public static ChatResponse from(Conversation conv, Message msg) {
        return from(conv, msg, List.of());
    }
}
