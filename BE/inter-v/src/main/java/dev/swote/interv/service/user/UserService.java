package dev.swote.interv.service.user;

import dev.swote.interv.domain.user.VO.RegisterVO;
import dev.swote.interv.domain.user.entity.User;
import dev.swote.interv.domain.user.repository.UserRepository;
import dev.swote.interv.util.security.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.NoSuchAlgorithmException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User register(RegisterVO registerVO) {
        // Check if email already exists
        userRepository.findByEmail(registerVO.getEmail())
                .ifPresent(user -> {
                    throw new RuntimeException("Email already registered");
                });

        try {
            User user = User.of(registerVO, passwordEncoder);
            return userRepository.save(user);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to register user", e);
        }
    }

    @Transactional(readOnly = true)
    public User getUserById(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional
    public User updateUser(Integer userId, User updatedUser) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update only allowed fields
        user.setNickname(updatedUser.getNickname());
        user.setPhoneNumber(updatedUser.getPhoneNumber());

        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.delete();
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}