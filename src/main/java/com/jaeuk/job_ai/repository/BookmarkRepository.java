package com.jaeuk.job_ai.repository;

import com.jaeuk.job_ai.entity.Bookmark;
import com.jaeuk.job_ai.entity.Message;
import com.jaeuk.job_ai.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    Optional<Bookmark> findByUserAndMessage(User user, Message message);

    List<Bookmark> findByUserOrderByCreatedAtDesc(User user);

    boolean existsByUserAndMessage(User user, Message message);
}
