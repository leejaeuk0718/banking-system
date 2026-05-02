package com.jaeuk.job_ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String BLACKLIST_PREFIX = "blacklist:";
    private static final String LOGIN_PREFIX = "login:";      // 추가

    public void addToBlacklist(String token, long expirationTime) {
        redisTemplate.opsForValue().set(BLACKLIST_PREFIX + token, true, expirationTime, TimeUnit.MILLISECONDS);
    }

    public boolean isBlacklisted(String token) {
        Boolean isBlacklisted = (Boolean) redisTemplate.opsForValue().get(BLACKLIST_PREFIX + token);
        return isBlacklisted != null && isBlacklisted;
    }

    // 로그인 시 토큰 저장
    public void saveLoginToken(String email, String token, long expirationTime) {
        redisTemplate.opsForValue().set(LOGIN_PREFIX + email, token, expirationTime, TimeUnit.MILLISECONDS);
    }

    // 기존 로그인 토큰 조회
    public String getLoginToken(String email) {
        return (String) redisTemplate.opsForValue().get(LOGIN_PREFIX + email);
    }

    // 로그아웃 시 로그인 토큰 삭제
    public void removeLoginToken(String email) {
        redisTemplate.delete(LOGIN_PREFIX + email);
    }
}