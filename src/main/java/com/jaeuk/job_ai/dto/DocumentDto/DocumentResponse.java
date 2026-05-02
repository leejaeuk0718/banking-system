package com.jaeuk.job_ai.dto.DocumentDto;

import com.jaeuk.job_ai.entity.Document;
import com.jaeuk.job_ai.enums.DocumentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponse {

    private Long id;
    private String originalFilename;
    private String contentType;
    private long sizeBytes;
    private String source;
    private DocumentStatus status;
    private int chunkCount;
    private String errorMessage;
    private LocalDateTime createdAt;

    public static DocumentResponse from(Document d) {
        return DocumentResponse.builder()
                .id(d.getId())
                .originalFilename(d.getOriginalFilename())
                .contentType(d.getContentType())
                .sizeBytes(d.getSizeBytes())
                .source(d.getSource())
                .status(d.getStatus())
                .chunkCount(d.getChunkCount())
                .errorMessage(d.getErrorMessage())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
