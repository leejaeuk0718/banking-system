package com.jaeuk.job_ai.service;

import com.jaeuk.job_ai.dto.BookmarkDto.BookmarkRequest;
import com.jaeuk.job_ai.dto.BookmarkDto.BookmarkResponse;
import com.jaeuk.job_ai.entity.Bookmark;
import com.jaeuk.job_ai.entity.Message;
import com.jaeuk.job_ai.entity.User;
import com.jaeuk.job_ai.repository.BookmarkRepository;
import com.jaeuk.job_ai.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final MessageRepository messageRepository;

    /** 내 북마크 목록 (최신순). */
    public List<BookmarkResponse> listMyBookmarks(User user) {
        return bookmarkRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(BookmarkResponse::from)
                .toList();
    }

    /**
     * 북마크 추가. 이미 북마크한 메시지면 {@link IllegalStateException} 던짐.
     * (프론트에서 토글 버튼을 쓰는 경우 delete 를 먼저 호출하게 유도)
     */
    @Transactional
    public BookmarkResponse create(User user, BookmarkRequest request) {
        Message message = messageRepository.findById(request.getMessageId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "메시지를 찾을 수 없습니다: " + request.getMessageId()));

        if (bookmarkRepository.existsByUserAndMessage(user, message)) {
            throw new IllegalStateException("이미 북마크한 메시지입니다: " + request.getMessageId());
        }

        Bookmark bookmark = bookmarkRepository.save(
                Bookmark.of(user, message, request.getNote()));
        return BookmarkResponse.from(bookmark);
    }

    /** 북마크 삭제 (본인 것만). */
    @Transactional
    public void delete(User user, Long bookmarkId) {
        Bookmark bookmark = bookmarkRepository.findById(bookmarkId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "북마크를 찾을 수 없습니다: " + bookmarkId));
        if (!bookmark.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("북마크를 찾을 수 없습니다: " + bookmarkId);
        }
        bookmarkRepository.delete(bookmark);
    }
}
