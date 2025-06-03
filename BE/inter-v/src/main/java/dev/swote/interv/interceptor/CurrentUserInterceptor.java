package dev.swote.interv.interceptor;

import dev.swote.interv.domain.user.entity.User;
import dev.swote.interv.domain.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
public class CurrentUserInterceptor implements HandlerInterceptor {
    private final UserRepository userRepository;

    public CurrentUserInterceptor(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // AUTH_USER_ID 헤더로 간단한 사용자 식별 (Cognito 대체)
        final String authUserId = request.getHeader("AUTH_USER_ID");
        if (authUserId == null) return true;

        try {
            User user = userRepository.findById(Integer.parseInt(authUserId)).orElse(null);
            if (user == null) return true;

            final CurrentUser currentUser = CurrentUser.from(user);
            request.setAttribute("currentUser", currentUser);
        } catch (NumberFormatException e) {
            log.warn("Invalid AUTH_USER_ID format: {}", authUserId);
        }

        return HandlerInterceptor.super.preHandle(request, response, handler);
    }
}