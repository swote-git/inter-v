package dev.swote.interv.util.security;

import dev.swote.interv.service.user.CognitoUserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import dev.swote.interv.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtDecoder jwtDecoder;
    private final UserRepository userRepository;
    private final CognitoUserService cognitoUserService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String token = getJwtFromRequest(request);

            if (StringUtils.hasText(token)) {
                Jwt jwt = jwtDecoder.decode(token);

                // 사용자 기본 정보 가져오기
                String cognitoUsername = jwt.getSubject();

                // 사용자 권한 추출
                Collection<SimpleGrantedAuthority> authorities = getAuthorities(jwt);

                // 내부 사용자 정보 조회 또는 생성
                UserDetails userDetails = cognitoUserService.loadOrCreateUser(cognitoUsername, jwt);

                // 인증 객체 생성 및 SecurityContext에 설정
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // 현재 사용자 ID를 요청 속성으로 설정 (기존 인터셉터와 통합)
                request.setAttribute("AUTH_USER_ID", userDetails.getUsername());
            }
        } catch (JwtException e) {
            log.error("JWT 토큰 유효성 검증 실패: {}", e.getMessage());
            // 인증 실패 시 SecurityContext를 클리어하지 않고 진행하여 필터 체인에서 처리되게 함
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

    private Collection<SimpleGrantedAuthority> getAuthorities(Jwt jwt) {
        // Cognito 그룹 또는 커스텀 클레임에서 권한 정보 추출
        Object groups = jwt.getClaims().get("cognito:groups");

        if (groups instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> groupList = (List<String>) groups;
            return groupList.stream()
                    .map(group -> new SimpleGrantedAuthority("ROLE_" + group.toUpperCase()))
                    .collect(Collectors.toList());
        }

        // 기본 사용자 권한
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }
}