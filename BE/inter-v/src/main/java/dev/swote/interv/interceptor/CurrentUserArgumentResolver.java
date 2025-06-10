package dev.swote.interv.interceptor;

import dev.swote.interv.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Slf4j
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(CurrentUser.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {

        CurrentUser currentUser = (CurrentUser) ((HttpServletRequest) webRequest.getNativeRequest())
                .getAttribute("currentUser");

        // CurrentUser가 null이고 파라미터가 필수인 경우 예외 처리
        if (currentUser == null && !parameter.isOptional()) {
            throw new UnauthorizedException();
        }

        return currentUser;
    }
}