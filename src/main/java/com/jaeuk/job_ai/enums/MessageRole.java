package com.jaeuk.job_ai.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MessageRole {

    USER("사용자"),
    ASSISTANT("AI");

    private final String displayName;
}
