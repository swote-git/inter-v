package dev.swote.interv.config;

import dev.swote.interv.interceptor.RestTemplateLoggingInterceptor;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Rest Template 설정
 * @author swote
 */
@Configuration
@RequiredArgsConstructor
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
        HttpClientConnectionManager manager = PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultConnectionConfig(
                        ConnectionConfig.custom()
                                .setSocketTimeout(4000, TimeUnit.MILLISECONDS)
                                .setConnectTimeout(5000, TimeUnit.MILLISECONDS)
                                .build()
                )
                .setMaxConnTotal(50)
                .setMaxConnPerRoute(20)
                .build();

        HttpClient c = HttpClientBuilder.create()
                .setConnectionManager(manager)
                .build();

        HttpComponentsClientHttpRequestFactory f = new HttpComponentsClientHttpRequestFactory();
        f.setHttpClient(c);

        f.setConnectTimeout(3000); // 연결 시도 타임아웃 (ms)
        f.setReadTimeout(60000); // 응답 대기 타임아웃 (ms) - 60초로 확장

        return restTemplateBuilder
                .requestFactory(() -> new BufferingClientHttpRequestFactory(f))
                .additionalMessageConverters(new StringHttpMessageConverter(StandardCharsets.UTF_8))
                .additionalInterceptors(new RestTemplateLoggingInterceptor())
                .build();
    }

}