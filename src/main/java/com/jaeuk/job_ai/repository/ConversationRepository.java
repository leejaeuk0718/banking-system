package com.jaeuk.job_ai.repository;

import com.jaeuk.job_ai.entity.Conversation;
import com.jaeuk.job_ai.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    /** 사이드바 "최근 대화" 목록 — 마지막 메시지 시각 내림차순 */
    List<Conversation> findByUserOrderByLastMessageAtDesc(User user);
}
