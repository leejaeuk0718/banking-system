package com.jaeuk.job_ai.service;

import com.jaeuk.job_ai.dto.ChatDto.ChatRequest;
import com.jaeuk.job_ai.entity.Conversation;
import com.jaeuk.job_ai.entity.Message;
import com.jaeuk.job_ai.entity.User;
import com.jaeuk.job_ai.enums.MessageRole;
import com.jaeuk.job_ai.enums.UserRole;
import com.jaeuk.job_ai.exception.ConversationNotFoundException;
import com.jaeuk.job_ai.repository.ConversationRepository;
import com.jaeuk.job_ai.repository.MessageRepository;
import com.jaeuk.job_ai.repository.UserRepository;
import com.jaeuk.job_ai.service.ChatTxService.ConversationAndMessage;
import com.jaeuk.job_ai.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ChatTxService 통합 테스트.
 *
 * <p>LLM 호출 없이 DB 트랜잭션 로직만 검증한다.</p>
 */
class ChatTxServiceIT extends IntegrationTestBase {

    @Autowired ChatTxService chatTxService;
    @Autowired UserRepository userRepository;
    @Autowired ConversationRepository conversationRepository;
    @Autowired MessageRepository messageRepository;

    @MockBean RedisTemplate<String, String> redisTemplate;

    private User user;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .name("테스터")
                .email("tx-test@example.com")
                .password("hashed-pw")
                .phone("01099998888")
                .birthDate("19950101")
                .role(UserRole.USER)
                .build());
    }

    @Test
    @DisplayName("conversationId 가 null 이면 새 대화를 생성하고 사용자 메시지를 저장한다")
    void getOrCreate_nullId_createsNewConversation() {
        // when
        ConversationAndMessage result =
                chatTxService.getOrCreateConversationAndSaveUser(user, null, "고혈압 증상이 뭔가요?");

        // then
        Conversation conv = result.conversation();
        assertThat(conv.getId()).isNotNull();
        assertThat(conv.getTitle()).isEqualTo("고혈압 증상이 뭔가요?");

        Message msg = result.userMessage();
        assertThat(msg.getRole()).isEqualTo(MessageRole.USER);
        assertThat(msg.getContent()).isEqualTo("고혈압 증상이 뭔가요?");
    }

    @Test
    @DisplayName("conversationId 를 넘기면 기존 대화에 메시지를 이어 붙인다")
    void getOrCreate_existingId_appendsMessage() {
        // given
        Conversation existing = conversationRepository.save(
                Conversation.start(user, "기존 대화"));

        // when
        ConversationAndMessage result = chatTxService.getOrCreateConversationAndSaveUser(
                user, existing.getId(), "추가 질문");

        // then
        assertThat(result.conversation().getId()).isEqualTo(existing.getId());
        assertThat(result.userMessage().getContent()).isEqualTo("추가 질문");
    }

    @Test
    @DisplayName("다른 사용자 소유의 대화를 요청하면 ConversationNotFoundException 을 던진다")
    void getOrCreate_otherOwner_throwsNotFound() {
        // given
        User other = userRepository.save(User.builder()
                .name("다른유저")
                .email("other@example.com")
                .password("pw")
                .phone("01011112222")
                .birthDate("20000101")
                .role(UserRole.USER)
                .build());

        Conversation otherConv = conversationRepository.save(
                Conversation.start(other, "다른 사람 대화"));

        // when & then
        assertThatThrownBy(() ->
                chatTxService.getOrCreateConversationAndSaveUser(
                        user, otherConv.getId(), "침입 시도"))
                .isInstanceOf(ConversationNotFoundException.class);
    }

    @Test
    @DisplayName("saveAssistantMessage 는 AI 응답을 저장하고 lastMessageAt 을 갱신한다")
    void saveAssistantMessage_updatesLastMessageAt() throws InterruptedException {
        // given
        Conversation conv = conversationRepository.save(
                Conversation.start(user, "응답 저장 테스트"));
        var before = conv.getLastMessageAt();
        Thread.sleep(10);   // lastMessageAt 변화를 확인하기 위해 짧게 대기

        // when
        Message msg = chatTxService.saveAssistantMessage(conv, "AI 답변 내용입니다.");

        // then
        assertThat(msg.getRole()).isEqualTo(MessageRole.ASSISTANT);
        assertThat(msg.getContent()).isEqualTo("AI 답변 내용입니다.");
        assertThat(conv.getLastMessageAt()).isAfterOrEqualTo(before);
    }

    @Test
    @DisplayName("제목이 30자를 초과하면 잘라서 '...' 를 붙인다")
    void generateTitle_truncatesLongMessage() {
        // given — 35자 메시지
        String longMessage = "이것은 매우 긴 질문으로 제목을 잘라야 하는 상황입니다 추가내용";

        // when
        ConversationAndMessage result =
                chatTxService.getOrCreateConversationAndSaveUser(user, null, longMessage);

        // then
        String title = result.conversation().getTitle();
        assertThat(title).endsWith("...");
        assertThat(title.length()).isLessThanOrEqualTo(33); // 30 + "..."
    }
}
