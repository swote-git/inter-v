package dev.swote.interv.config;

import dev.swote.interv.domain.user.repository.UserRepository;
import dev.swote.interv.interceptor.CurrentUserArgumentResolver;
import dev.swote.interv.interceptor.CurrentUserInterceptor;
import dev.swote.interv.interceptor.RequestInfoInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final UserRepository userRepository;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new CurrentUserArgumentResolver());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RequestInfoInterceptor());
        registry.addInterceptor(new CurrentUserInterceptor(userRepository));
    }
}