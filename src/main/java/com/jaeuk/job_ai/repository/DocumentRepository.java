package com.jaeuk.job_ai.repository;

import com.jaeuk.job_ai.entity.Document;
import com.jaeuk.job_ai.enums.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findAllByOrderByCreatedAtDesc();

    List<Document> findByStatusOrderByCreatedAtDesc(DocumentStatus status);
}
