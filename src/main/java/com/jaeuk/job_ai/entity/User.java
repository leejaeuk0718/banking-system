package com.jaeuk.job_ai.entity;

import com.jaeuk.job_ai.dto.UserDto.UserRequest;
import com.jaeuk.job_ai.enums.UserRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;                    // 실명

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true, length = 20)
    private String phone;                   // 전화번호 (본인인증 용도)

    @Column(nullable = false, length = 10)
    private String birthDate;              // 생년월일 (YYYYMMDD)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;                 // USER, ADMIN

    // 생성 메서드 (정적 팩토리)
    public static User from(UserRequest userRequest) {
        return User.builder()
                .name(userRequest.getName())
                .email(userRequest.getEmail())
                .password(userRequest.getPassword())
                .phone(userRequest.getPhone())
                .birthDate(userRequest.getBirthDate())
                .role(UserRole.USER)
                .build();
    }

    public void update(String name,
                       String password,
                       @NotBlank(message = "전화번호는 필수입니다")
                       @Pattern(regexp = "^01[0-9]{8,9}$", message = "올바른 전화번호 형식이 아닙니다")
                       String phone) {
        this.name = name;
        this.password = password;
        this.phone = phone;
    }
}
