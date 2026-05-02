package com.jaeuk.job_ai.repository;

import com.jaeuk.job_ai.entity.UsageLog;
import com.jaeuk.job_ai.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UsageLogRepository extends JpaRepository<UsageLog, Long> {

    List<UsageLog> findByUserOrderByCreatedAtDesc(User user);

    /** 사용자별 누적 토큰 합산 (관리자 대시보드용). */
    @Query("SELECT SUM(u.promptTokens + u.completionTokens) FROM UsageLog u WHERE u.user = :user")
    Long sumTotalTokensByUser(User user);
}
