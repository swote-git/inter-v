package dev.swote.interv.service.user;

import dev.swote.interv.domain.user.VO.RegisterVO;
import dev.swote.interv.domain.user.entity.User;
import dev.swote.interv.domain.user.repository.UserRepository;
import dev.swote.interv.exception.AuthenticationFailedException;
import dev.swote.interv.exception.DuplicateEmailException;
import dev.swote.interv.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User register(RegisterVO registerVO) {
        // 이메일 중복 체크
        userRepository.findByEmail(registerVO.getEmail())
                .ifPresent(user -> {
                    throw new DuplicateEmailException(registerVO.getEmail());
                });

        // 사용자 생성
        User user = User.builder()
                .email(registerVO.getEmail())
                .password(passwordEncoder.encode(registerVO.getPassword()))
                .userName(registerVO.getUserName())
                .nickname(registerVO.getNickname())
                .phoneNumber(registerVO.getPhoneNumber())
                .birthDate(registerVO.getBirthDate())
                .favoritedQuestions(new HashSet<>())
                .build();

        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User getUserById(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    @Transactional
    public User updateUser(Integer userId, User updatedUser) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // 허용된 필드만 업데이트
        if (updatedUser.getNickname() != null) {
            user.setNickname(updatedUser.getNickname());
        }
        if (updatedUser.getPhoneNumber() != null) {
            user.setPhoneNumber(updatedUser.getPhoneNumber());
        }
        if (updatedUser.getUserName() != null) {
            user.setUserName(updatedUser.getUserName());
        }

        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        user.delete();
        userRepository.save(user);
    }

    @Transactional
    public void changePassword(Integer userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new AuthenticationFailedException("error.auth.incorrectPassword");
        }

        // 새 비밀번호 설정
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}