package dev.swote.interv.util.security;

import dev.swote.interv.domain.user.entity.User;
import dev.swote.interv.domain.user.repository.UserRepository;
import dev.swote.interv.service.auth.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final AuthService authService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String token = getJwtFromRequest(request);

            if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {

                // 토큰이 블랙리스트에 있는지 확인
                if (authService.isTokenBlacklisted(token)) {
                    log.warn("블랙리스트에 있는 토큰입니다: {}", token.substring(0, 20) + "...");
                    filterChain.doFilter(request, response);
                    return;
                }

                // 액세스 토큰인지 확인
                String tokenType = jwtTokenProvider.getTokenTypeFromToken(token);
                if (!"access".equals(tokenType)) {
                    log.warn("액세스 토큰이 아닙니다: {}", tokenType);
                    filterChain.doFilter(request, response);
                    return;
                }

                String email = jwtTokenProvider.getEmailFromToken(token);
                Integer userId = jwtTokenProvider.getUserIdFromToken(token);

                // 사용자 정보 조회
                User user = userRepository.findByEmail(email).orElse(null);
                if (user != null && user.getId().equals(userId)) {

                    // 권한 설정
                    List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                            new SimpleGrantedAuthority("ROLE_USER"));

                    // 인증 객체 생성 및 SecurityContext에 설정
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(user, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    // 현재 사용자 ID를 요청 속성으로 설정 (기존 인터셉터와 통합)
                    request.setAttribute("AUTH_USER_ID", userId.toString());

                    log.debug("JWT 인증 성공: {}", email);
                }
            }
        } catch (Exception e) {
            log.error("JWT 인증 처리 중 오류 발생: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}