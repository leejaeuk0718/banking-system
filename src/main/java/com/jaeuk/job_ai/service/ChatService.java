package com.jaeuk.job_ai.service;

import com.jaeuk.job_ai.dto.ChatDto.ChatRequest;
import com.jaeuk.job_ai.dto.ChatDto.ChatResponse;
import com.jaeuk.job_ai.dto.ChatDto.ConversationDetailResponse;
import com.jaeuk.job_ai.dto.ChatDto.ConversationSummaryResponse;
import com.jaeuk.job_ai.dto.ChatDto.SourceChip;
import com.jaeuk.job_ai.entity.Conversation;
import com.jaeuk.job_ai.entity.Message;
import com.jaeuk.job_ai.entity.User;
import com.jaeuk.job_ai.enums.MessageRole;
import com.jaeuk.job_ai.service.ChatTxService.ConversationAndMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatTxService chatTxService;
    private final ChatClient chatClient;
    private final UsageLogService usageLogService;
    private final VectorStore vectorStore; // 🔥 추가

    public ChatResponse chat(User user, ChatRequest request) {

        // ── TX-1: 사용자 메시지 저장 ──────────────────
        ConversationAndMessage saved =
                chatTxService.getOrCreateConversationAndSaveUser(
                        user, request.getConversationId(), request.getMessage());

        Conversation conversation = saved.conversation();

        // ── 히스토리 로드 ─────────────────────────────
        List<Message> historyAsc = chatTxService.loadRecentHistory(conversation);

        // ── RAG: Vector 검색 ─────────────────────────
        List<Document> retrievedDocs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(request.getMessage())
                        .topK(4)
                        .similarityThreshold(0.5)
                        .build()
        );

        // ── Context 생성 ─────────────────────────────
        String context = retrievedDocs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n"));

        // ── Prompt 구성 ──────────────────────────────
        String prompt = """
                다음 CONTEXT를 기반으로 질문에 답변하세요.

                CONTEXT:
                %s

                질문:
                %s
                """.formatted(context, request.getMessage());

        // ── LLM 호출 ────────────────────────────────
        long llmStart = System.currentTimeMillis();

        var clientResponse = chatClient.prompt()
                .messages(toSpringAiMessages(historyAsc))
                .user(prompt)
                .call()
                .chatClientResponse();

        long latencyMs = System.currentTimeMillis() - llmStart;

        // ── 응답 추출 ────────────────────────────────
        var chatResponse = clientResponse.chatResponse();
        String aiText = chatResponse.getResult().getOutput().getText();

        // ── Source 생성 (직접 만든 retrievedDocs 사용) ─
        List<SourceChip> sources = retrievedDocs.stream()
                .map(SourceChip::from)
                .toList();

        // ── 토큰 사용량 ──────────────────────────────
        var usage = chatResponse.getMetadata().getUsage();
        int promptTokens     = usage != null ? (int) usage.getPromptTokens()     : 0;
        int completionTokens = usage != null ? (int) usage.getCompletionTokens() : 0;

        // ── TX-2: AI 응답 저장 ───────────────────────
        Message assistantMessage =
                chatTxService.saveAssistantMessage(conversation, aiText);

        // ── Async 로그 ──────────────────────────────
        usageLogService.saveAsync(
                user,
                assistantMessage,
                promptTokens,
                completionTokens,
                latencyMs,
                retrievedDocs.size()
        );

        log.debug("chat: conv={} latency={}ms promptTok={} completionTok={} sources={}",
                conversation.getId(), latencyMs, promptTokens, completionTokens, sources.size());

        return ChatResponse.from(conversation, assistantMessage, sources);
    }

    // ── 사이드바 ───────────────────────────────────
    public List<ConversationSummaryResponse> listConversations(User user) {
        return chatTxService.findAllByUser(user).stream()
                .map(ConversationSummaryResponse::from)
                .toList();
    }

    public ConversationDetailResponse getConversation(User user, Long conversationId) {
        Conversation conversation = chatTxService.findOwned(user, conversationId);
        List<Message> messages = chatTxService.findAllMessages(conversation);
        return ConversationDetailResponse.of(conversation, messages);
    }

    public void renameConversation(User user, Long conversationId, String newTitle) {
        chatTxService.rename(user, conversationId, newTitle);
    }

    public void deleteConversation(User user, Long conversationId) {
        chatTxService.deleteConversation(user, conversationId);
    }

    // ── 메시지 변환 ────────────────────────────────
    private List<org.springframework.ai.chat.messages.Message> toSpringAiMessages(List<Message> history) {
        return history.stream()
                .map(m -> (org.springframework.ai.chat.messages.Message)
                        (m.getRole() == MessageRole.USER
                                ? new UserMessage(m.getContent())
                                : new AssistantMessage(m.getContent())))
                .toList();
    }
}