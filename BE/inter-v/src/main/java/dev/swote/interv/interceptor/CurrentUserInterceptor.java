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
        final String authUserId = request.getHeader("AUTH_USER_ID");
        if (authUserId == null) return true;

        User user = userRepository.findById(Integer.parseInt(authUserId)).orElse(null);
        if (user == null) return true;

        final CurrentUser currentUser = CurrentUser.from(user);
        request.setAttribute("currentUser", currentUser);

        return HandlerInterceptor.super.preHandle(request, response, handler);
    }
}