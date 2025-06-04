package dev.swote.interv.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${spring.profiles.active:local}")
    private String activeProfile;

    @Value("${cors.allowed-origins:}")
    private String allowedOrigins;

    @Bean
    @Primary
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Health check 및 기본 경로
                        .requestMatchers("/", "/health", "/actuator/**").permitAll()
                        // Swagger UI 관련 경로 허용
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()
                        // 모든 API 엔드포인트를 공개로 설정
                        .anyRequest().permitAll()
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        if ("local".equals(activeProfile) || "dev".equals(activeProfile)) {
            log.info("개발 환경 CORS 설정 적용");
            configuration.setAllowedOriginPatterns(Arrays.asList("*"));
            configuration.setAllowCredentials(true);
        } else {
            log.info("운영 환경 CORS 설정 적용");

            // 환경변수에서 허용 도메인 읽기, 없으면 기본값 사용
            List<String> origins;
            if (allowedOrigins != null && !allowedOrigins.trim().isEmpty()) {
                origins = Arrays.asList(allowedOrigins.split(","));
                log.info("환경변수에서 읽은 허용 도메인: {}", origins);
            } else {
                origins = Arrays.asList(
                        "https://api.interv.swote.dev",
                        "https://app.interv.swote.dev",
                        "https://ml.interv.swote.dev",
                        "https://interv.swote.dev",
                        "https://www.interv.swote.dev",
                        // 임시로 모든 https 도메인 허용 (보안상 주의)
                        "https://*.swote.dev",
                        "http://localhost:*",
                        "https://localhost:*",
                        "http://127.0.0.1:*"
                );
                log.info("기본 허용 도메인 사용: {}", origins);
            }

            // HTTPS 도메인들과 함께 패턴으로 설정
            configuration.setAllowedOriginPatterns(origins);
            configuration.setAllowCredentials(true);
        }

        // 모든 HTTP 메서드 허용
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD", "TRACE"
        ));

        // 모든 헤더 허용
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // 클라이언트에서 접근할 수 있는 헤더 설정
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers",
                "X-Total-Count",
                "X-Total-Pages"
        ));

        // preflight 요청 캐시 시간 설정 (운영환경에서는 더 길게)
        long maxAge = "prod".equals(activeProfile) ? 86400L : 3600L; // 24시간 vs 1시간
        configuration.setMaxAge(maxAge);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        log.info("CORS 설정 완료 - 활성 프로필: {}, MaxAge: {}초", activeProfile, maxAge);
        return source;
    }
}