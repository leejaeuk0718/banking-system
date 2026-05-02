package com.jaeuk.job_ai.exception;

import org.springframework.http.HttpStatus;

public class DocumentNotFoundException extends BaseException {

    public DocumentNotFoundException(Long id) {
        super("해당 문서를 찾을 수 없습니다. id: " + id, HttpStatus.NOT_FOUND);
    }
}
