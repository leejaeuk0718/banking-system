package com.jaeuk.job_ai.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jaeuk.job_ai.dto.UserDto.LoginUserRequest;
import com.jaeuk.job_ai.service.TokenBlacklistService;
import com.jaeuk.job_ai.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

@Slf4j
public class JwtLoginAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtUtil jwtUtil;

    public JwtLoginAuthenticationFilter(AuthenticationManager authenticationManager, TokenBlacklistService tokenBlacklistService, JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.tokenBlacklistService = tokenBlacklistService;
        this.jwtUtil = jwtUtil;
        setFilterProcessesUrl("/api/users/login");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request,
                                                HttpServletResponse response) throws AuthenticationException {
        try {
            LoginUserRequest loginRequest = new ObjectMapper()
                    .readValue(request.getInputStream(), LoginUserRequest.class);

            // username 자리에 email 사용
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword());

            return authenticationManager.authenticate(authToken);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain chain,
                                            Authentication authResult) throws IOException {
        // authResult에서 principal을 UserDetails로 가져옵니다.
        CustomUserDetails userDetails = (CustomUserDetails) authResult.getPrincipal();

        // JWT 토큰을 생성합니다.
        String accessToken = jwtUtil.generateToken(userDetails);

        // 토큰이 블랙리스트에 있는지 확인
        if (tokenBlacklistService.isBlacklisted(accessToken)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("{\"error\": \"Access Token is blacklisted.\"}");
            return;
        }

        // 블랙리스트에 없으면 계속 진행
        response.setHeader("Authorization", "Bearer " + accessToken);

        // Refresh Token 생성 및 쿠키에 추가
        String refreshToken = jwtUtil.generateRefreshToken(userDetails.getUsername());
        Cookie refreshTokenCookie = new Cookie("refresh_token", refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(jwtUtil.getRefreshExpirationSeconds());
        response.addCookie(refreshTokenCookie);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"accessToken\": \"" + accessToken + "\", \"name\": \"" + userDetails.getName() + "\"}");
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        log.info("Authentication failed: {}", failed.getMessage());
        super.unsuccessfulAuthentication(request, response, failed);
    }
}