package com.jaeuk.job_ai.controller;

import com.jaeuk.job_ai.dto.BookmarkDto.BookmarkRequest;
import com.jaeuk.job_ai.dto.BookmarkDto.BookmarkResponse;
import com.jaeuk.job_ai.security.CustomUserDetails;
import com.jaeuk.job_ai.service.BookmarkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Bookmark", description = "AI 답변 북마크 저장/조회/삭제")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/bookmarks")
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @GetMapping
    @Operation(summary = "내 북마크 목록", description = "최신 순으로 반환됩니다.")
    public ResponseEntity<List<BookmarkResponse>> list(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(bookmarkService.listMyBookmarks(userDetails.getUser()));
    }

    @PostMapping
    @Operation(summary = "북마크 추가")
    public ResponseEntity<BookmarkResponse> create(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody BookmarkRequest request) {
        return ResponseEntity.ok(bookmarkService.create(userDetails.getUser(), request));
    }

    @DeleteMapping("/{bookmarkId}")
    @Operation(summary = "북마크 삭제")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long bookmarkId) {
        bookmarkService.delete(userDetails.getUser(), bookmarkId);
        return ResponseEntity.noContent().build();
    }
}
