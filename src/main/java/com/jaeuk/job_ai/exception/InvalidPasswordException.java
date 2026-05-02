package com.jaeuk.job_ai.exception;

import org.springframework.http.HttpStatus;

/**
 * 현재 비밀번호 검증 실패. 회원 정보 수정/탈퇴 등에서 본인 확인이 어긋날 때 사용.
 * 401 UNAUTHORIZED — "당신은 인증된 세션을 가졌지만 이 작업을 위한 본인 확인은 통과하지 못했다" 의미.
 */
public class InvalidPasswordException extends BaseException {

    public InvalidPasswordException() {
        super("현재 비밀번호가 일치하지 않습니다", HttpStatus.UNAUTHORIZED);
    }

    public InvalidPasswordException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
