package com.jaeuk.job_ai.dto.UserDto;

import com.jaeuk.job_ai.entity.User;
import com.jaeuk.job_ai.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRequest {

    @NotBlank(message = "이름은 필수입니다")
    private String name;

    @Email(message = "이메일 형식이 아닙니다")
    @NotBlank(message = "이메일은 필수입니다")
    private String email;

    @NotBlank(message = "비밀번호는 필수 입력 값입니다")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[@#$%^&+=!])(?!.*\\s).{8,16}$",
            message = "비밀번호는 영문, 숫자, 특수문자 모두 포함해주세요.")
    private String password;

    @NotBlank(message = "전화번호는 필수입니다")
    @Pattern(regexp = "^01[0-9]{8,9}$", message = "올바른 전화번호 형식이 아닙니다")
    private String phone;

    @NotBlank(message = "생년월일은 필수입니다")
    @Pattern(regexp = "^\\d{8}$", message = "생년월일은 YYYYMMDD 형식으로 입력해주세요")
    private String birthDate;


    public static UserRequest from(User user) {
        return UserRequest.builder()
                .name(user.getName())
                .email(user.getEmail())
                .password(user.getPassword())
                .phone(user.getPhone())
                .birthDate(user.getBirthDate())
                .build();
    }
}