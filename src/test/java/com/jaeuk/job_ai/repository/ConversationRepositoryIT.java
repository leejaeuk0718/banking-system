package com.jaeuk.job_ai.repository;

import com.jaeuk.job_ai.entity.Conversation;
import com.jaeuk.job_ai.entity.Message;
import com.jaeuk.job_ai.entity.User;
import com.jaeuk.job_ai.enums.MessageRole;
import com.jaeuk.job_ai.enums.UserRole;
import com.jaeuk.job_ai.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Conversation / Message 리포지토리 통합 테스트.
 *
 * <p>PostgreSQL 컨테이너({@link IntegrationTestBase}) 위에서 실제 JPA 를 통해 검증한다.</p>
 */
@Transactional
class ConversationRepositoryIT extends IntegrationTestBase {

    @Autowired ConversationRepository conversationRepository;
    @Autowired MessageRepository messageRepository;
    @Autowired UserRepository userRepository;

    // Redis 는 테스트 환경에 없으므로 Mock 처리
    @MockBean RedisTemplate<String, String> redisTemplate;

    private User user;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .name("테스터")
                .email("test@example.com")
                .password("hashed-pw")
                .phone("01012345678")
                .birthDate("19900101")
                .role(UserRole.USER)
                .build());
    }

    @Test
    @DisplayName("대화 생성 후 목록을 lastMessageAt 내림차순으로 조회한다")
    void findByUser_orderedByLastMessageAt() {
        // given
        Conversation older = conversationRepository.save(Conversation.start(user, "오래된 대화"));
        Conversation newer = conversationRepository.save(Conversation.start(user, "최근 대화"));
        newer.touchLastMessageAt();   // newer 를 더 최근으로

        // when
        List<Conversation> result =
                conversationRepository.findByUserOrderByLastMessageAtDesc(user);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("최근 대화");
    }

    @Test
    @DisplayName("메시지 저장 후 대화 기준으로 시간 오름차순 조회된다")
    void findMessagesByConversation_ascOrder() {
        // given
        Conversation conv = conversationRepository.save(Conversation.start(user, "대화"));
        messageRepository.save(Message.of(conv, MessageRole.USER, "첫 번째 질문"));
        messageRepository.save(Message.of(conv, MessageRole.ASSISTANT, "첫 번째 답변"));
        messageRepository.save(Message.of(conv, MessageRole.USER, "두 번째 질문"));

        // when
        List<Message> messages = messageRepository.findByConversationOrderByCreatedAtAsc(conv);

        // then
        assertThat(messages).hasSize(3);
        assertThat(messages.get(0).getRole()).isEqualTo(MessageRole.USER);
        assertThat(messages.get(0).getContent()).isEqualTo("첫 번째 질문");
    }

    @Test
    @DisplayName("대화 삭제 시 메시지도 CASCADE 로 함께 삭제된다")
    void deleteConversation_cascadesMessages() {
        // given
        Conversation conv = conversationRepository.save(Conversation.start(user, "삭제 대화"));
        messageRepository.save(Message.of(conv, MessageRole.USER, "질문"));
        messageRepository.save(Message.of(conv, MessageRole.ASSISTANT, "답변"));

        Long convId = conv.getId();

        // when
        conversationRepository.delete(conv);
        // flush 해서 실제 DELETE 쿼리 실행
        conversationRepository.flush();

        // then
        assertThat(conversationRepository.findById(convId)).isEmpty();
        // @OnDelete(CASCADE) 로 DB 레벨에서 제거되므로 영속성 컨텍스트를 비워서 확인
        List<Message> remaining = messageRepository.findByConversationOrderByCreatedAtAsc(conv);
        assertThat(remaining).isEmpty();
    }

    @Test
    @DisplayName("컨텍스트 윈도우 — 최근 10개 메시지만 반환한다")
    void findTop10_returnsAtMostTen() {
        // given
        Conversation conv = conversationRepository.save(Conversation.start(user, "긴 대화"));
        for (int i = 0; i < 15; i++) {
            messageRepository.save(Message.of(conv, MessageRole.USER, "메시지 " + i));
        }

        // when
        List<Message> top10 =
                messageRepository.findTop10ByConversationOrderByCreatedAtDesc(conv);

        // then
        assertThat(top10).hasSize(10);
    }
}
