package dev.swote.interv.interceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
public class RequestInfoInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        log.info("\n===== REQUEST INFO =====\n       URI: {}\n       METHOD: {}\n=== REQUEST INFO END ===", uri, method);

        //FIXME: 필요 정보 알아서 추가
        return HandlerInterceptor.super.preHandle(request, response, handler);
    }
}