package dev.swote.interv.interceptor;


import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

@Slf4j
public class RestTemplateLoggingInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        URI uri = request.getURI();
        log.info("===== [RestTemplate <REQUEST> START] =====\n");
        log.info("==========> URI: {}\n", uri);
        log.info("==========> METHOD : {}\n", request.getMethod());
        log.info("==========> REQUEST BODY: {}\n", new String(body, StandardCharsets.UTF_8));
        log.info("===== [RestTemplate <REQUEST> END] =====");

        ClientHttpResponse response = execution.execute(request, body);

        //FIXME: 메모리가 부족할 경우 Response Body는 Debug 모드가 활성화 될때만 올리기.
        log.info("===== [RestTemplate <RESPONSE> START] =====\n");
        log.info("==========> STATUS: {}, {}\n", response.getStatusCode(), response.getStatusText());
        log.info("==========> RESPONSE BODY: {}\n", StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8));
        log.info("===== [RestTemplate <RESPONSE> END] =====");

        return response;
    }

}