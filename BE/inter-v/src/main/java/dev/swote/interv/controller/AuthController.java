package dev.swote.interv.controller;

import dev.swote.interv.config.CommonResponse;
import dev.swote.interv.domain.user.entity.User;
import dev.swote.interv.interceptor.CurrentUser;
import dev.swote.interv.service.auth.AuthService;
import dev.swote.interv.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<CommonResponse<Map<String, Object>>> login(
            @Valid @RequestBody LoginRequest request) {

        Map<String, Object> result = authService.authenticate(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(CommonResponse.ok(result));
    }

    @PostMapping("/logout")
    public ResponseEntity<CommonResponse<String>> logout(
            @RequestHeader(value = "Authorization", required = false) String token) {

        if (token != null && token.startsWith("Bearer ")) {
            String jwtToken = token.substring(7);
            authService.invalidateToken(jwtToken);
        }

        return ResponseEntity.ok(CommonResponse.ok("로그아웃 되었습니다."));
    }

    @GetMapping("/me")
    public ResponseEntity<CommonResponse<User>> getCurrentUser(CurrentUser currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userService.getUserById(currentUser.id());
        return ResponseEntity.ok(CommonResponse.ok(user));
    }

    @PostMapping("/refresh")
    public ResponseEntity<CommonResponse<Map<String, Object>>> refreshToken(
            @RequestBody RefreshTokenRequest request) {

        Map<String, Object> result = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(CommonResponse.ok(result));
    }

    // Request DTOs
    public static class LoginRequest {
        private String email;
        private String password;

        // Getters and setters
        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class RefreshTokenRequest {
        private String refreshToken;

        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }
    }
}