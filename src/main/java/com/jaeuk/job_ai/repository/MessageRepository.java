package com.jaeuk.job_ai.repository;

import com.jaeuk.job_ai.entity.Conversation;
import com.jaeuk.job_ai.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    /** 대화 상세 화면용 — 시간순 전체 메시지 */
    List<Message> findByConversationOrderByCreatedAtAsc(Conversation conversation);

    /**
     * LLM 컨텍스트 윈도우용 — 가장 최근 N 개를 가져온다.
     * (이전 코드의 findTop10*ByOrderByCreatedAtAsc 는 첫 10 개를 가져오는 버그였다.)
     * 호출 측에서 시간순으로 뒤집어 LLM 에 전달한다.
     */
    List<Message> findTop10ByConversationOrderByCreatedAtDesc(Conversation conversation);
}
