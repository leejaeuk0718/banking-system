package com.jaeuk.job_ai.service;

import com.jaeuk.job_ai.entity.Conversation;
import com.jaeuk.job_ai.entity.Message;
import com.jaeuk.job_ai.entity.User;
import com.jaeuk.job_ai.enums.MessageRole;
import com.jaeuk.job_ai.exception.ConversationNotFoundException;
import com.jaeuk.job_ai.repository.ConversationRepository;
import com.jaeuk.job_ai.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * ChatService 의 DB 전용 트랜잭션 헬퍼.
 *
 * <p>LLM 호출은 네트워크 I/O 가 수 초 이상 걸릴 수 있어, DB 커넥션을 그 시간만큼 붙들면 안 된다.
 * 따라서 ChatService 는 다음 순서로 이 클래스를 호출해 DB 커넥션 점유 시간을 최소화한다.</p>
 * <pre>
 *   [TX-1] getOrCreateConversation + saveUserMessage  → 커밋 → 커넥션 반납
 *   [LLM 호출 — 트랜잭션 없음]
 *   [TX-2] saveAssistantMessage + touchLastMessageAt  → 커밋
 *   [TX-3 async] saveUsageLog                         → 별도 커밋
 * </pre>
 */
@Service
@RequiredArgsConstructor
public class ChatTxService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    private static final int TITLE_MAX = 30;

    // ───────────────────────── TX-1 ─────────────────────────

    /**
     * conversationId 가 null 이면 새 대화를 만들고, 있으면 소유 검증 후 반환.
     * 사용자 메시지도 같은 트랜잭션에서 함께 저장해 원자성 보장.
     *
     * @return [conversation, userMessage] 튜플
     */
    @Transactional
    public ConversationAndMessage getOrCreateConversationAndSaveUser(
            User user, Long conversationId, String userText) {

        Conversation conversation = (conversationId != null)
                ? findOwned(user, conversationId)
                : conversationRepository.save(
                        Conversation.start(user, generateTitle(userText)));

        Message userMessage = messageRepository.save(
                Message.of(conversation, MessageRole.USER, userText));

        return new ConversationAndMessage(conversation, userMessage);
    }

    /**
     * LLM 호출 직전에 컨텍스트 윈도우(최근 N 개 메시지)를 읽어 반환.
     * 읽기 전용 트랜잭션이므로 커넥션 보유 시간이 짧다.
     */
    @Transactional(readOnly = true)
    public List<Message> loadRecentHistory(Conversation conversation) {
        List<Message> descOrder =
                messageRepository.findTop10ByConversationOrderByCreatedAtDesc(conversation);
        List<Message> asc = new ArrayList<>(descOrder);
        asc.sort(Comparator.comparing(Message::getCreatedAt));
        return asc;
    }

    // ───────────────────────── TX-2 ─────────────────────────

    /**
     * AI 응답 저장 + 대화 lastMessageAt 갱신. LLM 호출이 완료된 뒤 호출된다.
     *
     * <p>TX-1 에서 로드한 {@code conversation} 은 이미 detached 상태이므로,
     * 새 트랜잭션에서 ID 로 재조회해 변경감지가 동작하도록 한다.</p>
     */
    @Transactional
    public Message saveAssistantMessage(Conversation conversation, String aiText) {
        // detached 엔티티를 재조회해 영속 컨텍스트에 연결
        Conversation managed = conversationRepository.findById(conversation.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Conversation not found: " + conversation.getId()));
        Message msg = messageRepository.save(
                Message.of(managed, MessageRole.ASSISTANT, aiText));
        managed.touchLastMessageAt();
        return msg;
    }

    // ───────────────────────── 기존 조회/수정 ─────────────────────────

    @Transactional(readOnly = true)
    public Conversation findOwned(User user, Long conversationId) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
        if (!conv.isOwnedBy(user)) throw new ConversationNotFoundException(conversationId);
        return conv;
    }

    @Transactional(readOnly = true)
    public List<Conversation> findAllByUser(User user) {
        return conversationRepository.findByUserOrderByLastMessageAtDesc(user);
    }

    @Transactional(readOnly = true)
    public List<Message> findAllMessages(Conversation conversation) {
        return messageRepository.findByConversationOrderByCreatedAtAsc(conversation);
    }

    /**
     * 대화 이름 변경. ID 로 재조회해 같은 영속 컨텍스트 안에서 변경감지(dirty checking)가 동작하도록 한다.
     * 호출 측에서 별도 트랜잭션에 미리 로드한 detached 엔티티를 넘기면 변경이 반영되지 않는다.
     */
    @Transactional
    public void rename(User user, Long conversationId, String newTitle) {
        Conversation conv = findOwned(user, conversationId);
        conv.rename(newTitle);
    }

    /**
     * 대화 삭제. ID 로 재조회 후 DELETE 쿼리를 실행한다.
     */
    @Transactional
    public void deleteConversation(User user, Long conversationId) {
        Conversation conv = findOwned(user, conversationId);
        conversationRepository.delete(conv);
    }

    // ───────────────────────── 내부 ─────────────────────────

    private String generateTitle(String msg) {
        if (msg == null) return "새 대화";
        String t = msg.strip();
        if (t.isEmpty()) return "새 대화";
        return t.length() <= TITLE_MAX ? t : t.substring(0, TITLE_MAX) + "...";
    }

    // ─── 반환용 레코드 ───────────────────────────────────────

    public record ConversationAndMessage(Conversation conversation, Message userMessage) {}
}
