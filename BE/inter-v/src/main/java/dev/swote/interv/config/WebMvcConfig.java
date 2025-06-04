package dev.swote.interv.config;

import dev.swote.interv.domain.user.repository.UserRepository;
import dev.swote.interv.interceptor.CurrentUserArgumentResolver;
import dev.swote.interv.interceptor.CurrentUserInterceptor;
import dev.swote.interv.interceptor.RequestInfoInterceptor;
import dev.swote.interv.util.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final UserRepository userRepository;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${spring.profiles.active:local}")
    private String activeProfile;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new CurrentUserArgumentResolver());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RequestInfoInterceptor());
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration() {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(jwtAuthenticationFilter);
        registration.addUrlPatterns("/api/*");
        registration.setOrder(1);
        return registration;
    }

    // SecurityConfig에서 처리하고 있지만, 추가적인 CORS 설정을 위해 활성화
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if ("local".equals(activeProfile) || "dev".equals(activeProfile)) {
            registry.addMapping("/**")
                    .allowedOriginPatterns("*")
                    .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD")
                    .allowedHeaders("*")
                    .exposedHeaders("Authorization", "Content-Type")
                    .allowCredentials(true)
                    .maxAge(3600);
        }
    }
}