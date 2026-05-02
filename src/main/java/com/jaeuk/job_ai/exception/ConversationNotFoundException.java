package com.jaeuk.job_ai.exception;

import org.springframework.http.HttpStatus;

/**
 * 대화를 찾을 수 없거나 본인 소유가 아닐 때 사용.
 * 본인 소유가 아닌 경우에도 동일한 404 로 응답해 존재 여부 누설을 방지한다.
 */
public class ConversationNotFoundException extends BaseException {

    public ConversationNotFoundException(Long id) {
        super("해당 대화를 찾을 수 없습니다. id: " + id, HttpStatus.NOT_FOUND);
    }
}
