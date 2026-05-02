package com.jaeuk.job_ai.service;

import com.jaeuk.job_ai.dto.UserDto.UserRequest;
import com.jaeuk.job_ai.dto.UserDto.UserResponse;
import com.jaeuk.job_ai.dto.UserDto.UserSimpleRequest;
import com.jaeuk.job_ai.entity.User;
import com.jaeuk.job_ai.exception.InvalidPasswordException;
import com.jaeuk.job_ai.exception.UserNotFoundException;
import com.jaeuk.job_ai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder;

    @Transactional
    public UserResponse createUser(UserRequest userRequest) {
        if (userRepository.existsByEmail(userRequest.getEmail())) {
            throw new IllegalArgumentException("이미 사용중인 이메일입니다");
        }
        if (userRepository.existsByPhone(userRequest.getPhone())) {
            throw new IllegalArgumentException("이미 사용중인 전화번호입니다");
        }

        userRequest.setPassword(encoder.encode(userRequest.getPassword()));
        User user = User.from(userRequest);
        return UserResponse.from(userRepository.save(user));
    }

    /**
     * 본인 정보 수정.
     *  1) 저장된 비밀번호 해시와 {@code currentPassword} 를 비교해 본인 확인
     *  2) {@code newPassword} 가 비어 있으면 비밀번호는 기존 해시 유지, 값이 있으면 BCrypt 해시 후 교체
     *  3) 이름/전화번호는 항상 갱신
     */
    @Transactional
    public UserResponse updateUser(Long userId, UserSimpleRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 1) 본인 확인 — BCrypt.matches 는 timing-safe 비교
        if (!encoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidPasswordException();
        }

        // 2) 새 비밀번호 적용 여부 결정
        String passwordHashToApply = (request.getNewPassword() != null
                && !request.getNewPassword().isBlank())
                ? encoder.encode(request.getNewPassword())
                : user.getPassword();

        // 3) 엔티티 갱신
        user.update(request.getName(), passwordHashToApply, request.getPhone());

        return UserResponse.from(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        userRepository.delete(user);
    }

    public UserResponse getMe(User user) {
        return UserResponse.from(user);
    }
}
