package com.jaeuk.job_ai.dto.UserDto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

/**
 * 본인 정보 수정 요청.
 *
 * 보안 정책:
 *  - 어떤 변경이든 {@link #currentPassword} 로 본인 확인을 다시 받는다(세션 탈취 방어).
 *  - {@link #newPassword} 는 비밀번호를 실제로 바꿀 때만 채운다(빈 값이면 기존 비밀번호 유지).
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserSimpleRequest {

    @NotBlank(message = "이름은 필수입니다")
    private String name;

    @NotBlank(message = "본인 확인을 위해 현재 비밀번호가 필요합니다")
    private String currentPassword;

    /**
     * 새 비밀번호. {@code null} 또는 빈 문자열이면 비밀번호는 변경하지 않는다.
     * 값이 있을 때만 정규식 검증을 적용한다.
     */
    @Pattern(
            regexp = "^$|^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[@#$%^&+=!])(?!.*\\s).{8,16}$",
            message = "비밀번호는 영문, 숫자, 특수문자 모두 포함한 8~16자여야 합니다"
    )
    private String newPassword;

    @NotBlank(message = "전화번호는 필수입니다")
    @Pattern(regexp = "^01[0-9]{8,9}$", message = "올바른 전화번호 형식이 아닙니다")
    private String phone;
}
