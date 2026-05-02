package com.jaeuk.job_ai.repository;

import com.jaeuk.job_ai.entity.Feedback;
import com.jaeuk.job_ai.entity.Message;
import com.jaeuk.job_ai.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    Optional<Feedback> findByUserAndMessage(User user, Message message);

    /** 메시지별 LIKE/DISLIKE 집계용. */
    long countByMessageAndType(Message message, com.jaeuk.job_ai.enums.FeedbackType type);
}
