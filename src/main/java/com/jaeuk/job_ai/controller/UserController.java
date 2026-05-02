package com.jaeuk.job_ai.controller;

import com.jaeuk.job_ai.dto.UserDto.LoginUserRequest;
import com.jaeuk.job_ai.dto.UserDto.UserRequest;
import com.jaeuk.job_ai.dto.UserDto.UserResponse;
import com.jaeuk.job_ai.dto.UserDto.UserSimpleRequest;
import com.jaeuk.job_ai.security.CustomUserDetails;
import com.jaeuk.job_ai.service.TokenBlacklistService;
import com.jaeuk.job_ai.service.UserService;
import com.jaeuk.job_ai.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "User", description = "사용자 인증/계정 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final TokenBlacklistService tokenBlacklistService;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final UserService userService;

    /**
     * 회원가입
     */
    @PostMapping("/register")
    @Operation(summary = "회원가입")
    public ResponseEntity<?> register(@Validated @RequestBody UserRequest request,
                                      BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return buildValidationErrorResponse(bindingResult);
        }
        UserResponse userResponse = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(userResponse);
    }

    /**
     * 로그인 — Swagger 문서화 전용 스텁.
     * 실제 인증 처리는 {@code JwtLoginAuthenticationFilter} 가
     * {@code UsernamePasswordAuthenticationFilter} 보다 앞에 끼워져서 가로챈다.
     * 따라서 이 메서드 본문은 절대 실행되지 않는다 — Swagger UI 에서만 보인다.
     */
    @PostMapping("/login")
    @Operation(
            summary = "로그인",
            description = """
                    이메일/비밀번호로 인증을 수행하고 access 토큰(Header: Authorization)과
                    refresh 토큰(HttpOnly Cookie: refresh_token)을 발급한다.

                    실제 처리는 SecurityFilterChain 의 JwtLoginAuthenticationFilter 가 담당한다.
                    """
    )
    @ApiResponse(responseCode = "200", description = "로그인 성공 — Authorization 헤더에 Bearer 토큰")
    @ApiResponse(responseCode = "401", description = "인증 실패")
    public ResponseEntity<Void> loginDocStub(@RequestBody LoginUserRequest request) {
        // 도달 불가능 — JwtLoginAuthenticationFilter 가 선처리한다.
        throw new IllegalStateException(
                "Login is handled by JwtLoginAuthenticationFilter and should not reach the controller.");
    }

    /**
     * 로그아웃 — access 토큰을 블랙리스트에 등록.
     */
    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "현재 access 토큰을 블랙리스트에 등록한다")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        String token = jwtUtil.extractToken(request);
        if (token != null) {
            long expirationTime = jwtUtil.getExpirationTimeFromToken(token);
            tokenBlacklistService.addToBlacklist(token, expirationTime);
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok("로그아웃 되었습니다");
    }

    /**
     * 내 정보 조회
     */
    @GetMapping("/me")
    @Operation(summary = "내 정보 조회")
    public ResponseEntity<UserResponse> getMe(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UserResponse response = userService.getMe(userDetails.getUser());
        return ResponseEntity.ok(response);
    }

    /**
     * 내 정보 수정 — 인증된 사용자 자신만 수정 가능.
     * 임의의 userId 를 받지 않는다(권한 우회 방지).
     */
    @PutMapping("/me")
    @Operation(
            summary = "내 정보 수정",
            description = """
                    이름/전화번호를 갱신한다. 비밀번호 변경은 newPassword 가 채워졌을 때만 일어난다.
                    어떤 변경이든 currentPassword 로 본인 확인을 한 번 더 받는다(세션 탈취 방어).
                    """
    )
    @ApiResponse(responseCode = "200", description = "수정 성공 — 갱신된 사용자 정보 반환")
    @ApiResponse(responseCode = "400", description = "입력값 검증 실패")
    @ApiResponse(responseCode = "401", description = "현재 비밀번호 불일치")
    public ResponseEntity<?> updateMe(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Validated @RequestBody UserSimpleRequest request,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return buildValidationErrorResponse(bindingResult);
        }
        UserResponse updated = userService.updateUser(userDetails.getUser().getId(), request);
        return ResponseEntity.ok(updated);
    }

    /**
     * 회원 탈퇴 — 인증된 사용자 자신만 가능.
     * 탈퇴와 동시에 현재 access 토큰을 블랙리스트에 등록하고
     * SecurityContext 를 비워 즉시 로그아웃 효과까지 적용한다.
     */
    @DeleteMapping("/me")
    @Operation(summary = "회원 탈퇴")
    public ResponseEntity<String> deleteMe(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest httpRequest) {
        String token = jwtUtil.extractToken(httpRequest);
        if (token != null) {
            long expirationTime = jwtUtil.getExpirationTimeFromToken(token);
            tokenBlacklistService.addToBlacklist(token, expirationTime);
        }

        userService.deleteUser(userDetails.getUser().getId());
        SecurityContextHolder.clearContext();

        return ResponseEntity.ok("회원 탈퇴가 완료되었습니다");
    }

    /**
     * refreshToken 으로 access 토큰 재발급
     */
    @PostMapping("/refresh-token")
    @Operation(summary = "access 토큰 재발급", description = "refresh_token 쿠키 기반으로 access 토큰 재발급")
    public ResponseEntity<?> refreshAccessToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No refresh token provided");
        }

        String refreshToken = Arrays.stream(cookies)
                .filter(cookie -> "refresh_token".equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);

        if (refreshToken == null || jwtUtil.isTokenExpired(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired refresh token");
        }

        if (tokenBlacklistService.isBlacklisted(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token is blacklisted.");
        }

        String username = jwtUtil.extractUsername(refreshToken);
        CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(username);

        String newAccessToken = jwtUtil.generateToken(userDetails);

        return ResponseEntity.ok("{\"accessToken\": \"" + newAccessToken + "\"}");
    }

    private ResponseEntity<?> buildValidationErrorResponse(BindingResult bindingResult) {
        List<String> errorMessages = bindingResult.getAllErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.toList());
        return ResponseEntity.badRequest().body(errorMessages);
    }
}
