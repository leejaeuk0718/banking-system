package com.jaeuk.job_ai.dto.BookmarkDto;

import com.jaeuk.job_ai.entity.Bookmark;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BookmarkResponse {

    private Long id;
    private Long messageId;
    private String messageContent;   // 미리보기용 — 전체 내용 포함
    private String note;
    private LocalDateTime createdAt;

    public static BookmarkResponse from(Bookmark bookmark) {
        return BookmarkResponse.builder()
                .id(bookmark.getId())
                .messageId(bookmark.getMessage().getId())
                .messageContent(bookmark.getMessage().getContent())
                .note(bookmark.getNote())
                .createdAt(bookmark.getCreatedAt())
                .build();
    }
}
