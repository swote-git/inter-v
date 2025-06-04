package dev.swote.interv.service.auth;

import dev.swote.interv.domain.user.entity.User;
import dev.swote.interv.domain.user.repository.UserRepository;
import dev.swote.interv.exception.AuthenticationFailedException;
import dev.swote.interv.exception.InvalidTokenException;
import dev.swote.interv.exception.ResourceNotFoundException;
import dev.swote.interv.util.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    // 간단한 토큰 블랙리스트 (실제 운영에서는 Redis 등을 사용)
    private final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();

    @Transactional(readOnly = true)
    public Map<String, Object> authenticate(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationFailedException("error.auth.invalidCredentials"));

        // 비밀번호 검증
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new AuthenticationFailedException("error.auth.invalidCredentials");
        }

        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(user.getEmail(), user.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());

        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", accessToken);
        result.put("refreshToken", refreshToken);
        result.put("tokenType", "Bearer");
        result.put("expiresIn", jwtTokenProvider.getAccessTokenValidityInSeconds());
        result.put("user", user);

        log.info("사용자 로그인 성공: {}", email);
        return result;
    }

    public Map<String, Object> refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new InvalidTokenException("error.token.invalid");
        }

        if (blacklistedTokens.contains(refreshToken)) {
            throw new InvalidTokenException("error.token.expired");
        }

        String email = jwtTokenProvider.getEmailFromToken(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        // 새로운 액세스 토큰 생성
        String newAccessToken = jwtTokenProvider.createAccessToken(user.getEmail(), user.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", newAccessToken);
        result.put("tokenType", "Bearer");
        result.put("expiresIn", jwtTokenProvider.getAccessTokenValidityInSeconds());

        return result;
    }

    public void invalidateToken(String token) {
        blacklistedTokens.add(token);
        log.info("토큰이 무효화되었습니다.");
    }

    public boolean isTokenBlacklisted(String token) {
        return blacklistedTokens.contains(token);
    }
}