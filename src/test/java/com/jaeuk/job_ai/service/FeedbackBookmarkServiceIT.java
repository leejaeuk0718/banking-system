package com.jaeuk.job_ai.service;

import com.jaeuk.job_ai.dto.BookmarkDto.BookmarkRequest;
import com.jaeuk.job_ai.dto.BookmarkDto.BookmarkResponse;
import com.jaeuk.job_ai.dto.FeedbackDto.FeedbackRequest;
import com.jaeuk.job_ai.dto.FeedbackDto.FeedbackResponse;
import com.jaeuk.job_ai.entity.Conversation;
import com.jaeuk.job_ai.entity.Message;
import com.jaeuk.job_ai.entity.User;
import com.jaeuk.job_ai.enums.FeedbackType;
import com.jaeuk.job_ai.enums.MessageRole;
import com.jaeuk.job_ai.enums.UserRole;
import com.jaeuk.job_ai.repository.ConversationRepository;
import com.jaeuk.job_ai.repository.MessageRepository;
import com.jaeuk.job_ai.repository.UserRepository;
import com.jaeuk.job_ai.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Feedback / Bookmark 서비스 통합 테스트.
 *
 * <p>DTO 에 @Setter 가 없으므로 리플렉션 헬퍼로 필드를 설정한다.
 * 실무에서는 DTO 에 테스트 전용 팩토리 메서드(@VisibleForTesting)를 두거나
 * ObjectMapper 를 통한 JSON 역직렬화 방식을 권장한다.</p>
 */
class FeedbackBookmarkServiceIT extends IntegrationTestBase {

    @Autowired FeedbackService feedbackService;
    @Autowired BookmarkService bookmarkService;
    @Autowired UserRepository userRepository;
    @Autowired ConversationRepository conversationRepository;
    @Autowired MessageRepository messageRepository;

    @MockBean RedisTemplate<String, String> redisTemplate;

    private User user;
    private Message assistantMessage;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .name("피드백유저")
                .email("fb@example.com")
                .password("pw")
                .phone("01033334444")
                .birthDate("19920101")
                .role(UserRole.USER)
                .build());

        Conversation conv = conversationRepository.save(Conversation.start(user, "테스트 대화"));
        messageRepository.save(Message.of(conv, MessageRole.USER, "질문"));
        assistantMessage = messageRepository.save(
                Message.of(conv, MessageRole.ASSISTANT, "AI 답변"));
    }

    // ── Feedback ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("피드백 LIKE 생성 후 DISLIKE 로 upsert 하면 type 이 변경된다")
    void feedback_upsert_changesType() {
        feedbackService.upsert(user, feedbackReq(FeedbackType.LIKE, null));

        FeedbackResponse result = feedbackService.upsert(
                user, feedbackReq(FeedbackType.DISLIKE, "부정확한 정보"));

        assertThat(result.getType()).isEqualTo(FeedbackType.DISLIKE);
        assertThat(result.getComment()).isEqualTo("부정확한 정보");
    }

    @Test
    @DisplayName("피드백 삭제 후 동일 메시지에 다시 upsert 하면 새 레코드가 생성된다")
    void feedback_delete_then_recreate() {
        FeedbackResponse created = feedbackService.upsert(
                user, feedbackReq(FeedbackType.LIKE, null));
        feedbackService.delete(user, created.getId());

        FeedbackResponse recreated = feedbackService.upsert(
                user, feedbackReq(FeedbackType.DISLIKE, null));

        assertThat(recreated.getId()).isNotEqualTo(created.getId());
    }

    // ── Bookmark ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("북마크 생성 후 목록 조회하면 해당 메시지 내용이 포함된다")
    void bookmark_create_and_list() {
        bookmarkService.create(user, bookmarkReq("나중에 다시 볼 것"));

        List<BookmarkResponse> list = bookmarkService.listMyBookmarks(user);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getMessageContent()).isEqualTo("AI 답변");
        assertThat(list.get(0).getNote()).isEqualTo("나중에 다시 볼 것");
    }

    @Test
    @DisplayName("같은 메시지에 북마크를 두 번 생성하면 IllegalStateException 이 발생한다")
    void bookmark_duplicate_throwsException() {
        bookmarkService.create(user, bookmarkReq(null));

        assertThatThrownBy(() -> bookmarkService.create(user, bookmarkReq(null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 북마크");
    }

    @Test
    @DisplayName("북마크 삭제 후 목록이 비어 있다")
    void bookmark_delete_emptyList() {
        BookmarkResponse created = bookmarkService.create(user, bookmarkReq(null));

        bookmarkService.delete(user, created.getId());

        assertThat(bookmarkService.listMyBookmarks(user)).isEmpty();
    }

    // ───────────────────────── DTO 빌더 헬퍼 ─────────────────────────────────

    private FeedbackRequest feedbackReq(FeedbackType type, String comment) {
        try {
            FeedbackRequest req = new FeedbackRequest();
            setField(req, "messageId", assistantMessage.getId());
            setField(req, "type", type);
            setField(req, "comment", comment);
            return req;
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private BookmarkRequest bookmarkReq(String note) {
        try {
            BookmarkRequest req = new BookmarkRequest();
            setField(req, "messageId", assistantMessage.getId());
            setField(req, "note", note);
            return req;
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private static void setField(Object obj, String fieldName, Object value) throws Exception {
        Field f = obj.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(obj, value);
    }
}
