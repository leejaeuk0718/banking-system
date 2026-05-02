package com.jaeuk.job_ai.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserRole {
    USER("ROLE_USER"),       // 일반 고객
    ADMIN("ROLE_ADMIN");     // 관리자

    private final String value;
}

