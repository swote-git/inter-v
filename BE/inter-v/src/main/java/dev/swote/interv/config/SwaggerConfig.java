package dev.swote.interv.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SwaggerConfig {

    @Value("${spring.profiles.active:local}")
    private String profile;

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${swagger.server.url:}")
    private String swaggerServerUrl;

    @Bean
    public OpenAPI openAPI() {
        final String keyName = "id";
        final Info info = new Info()
                .version("v0.0.1")
                .title(String.format("[PROFILE: %s] 클라우드컴퓨팅 Inter-V API", profile));

        final SecurityRequirement securityRequirement = new SecurityRequirement().addList(keyName);
        final SecurityScheme securityScheme = new SecurityScheme()
                .scheme(keyName)
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name("AUTH_USER_ID")
                .description("인증에 사용되는 유저 ID");

        final Components components = new Components().addSecuritySchemes(keyName, securityScheme);

        OpenAPI openAPI = new OpenAPI()
                .info(info)
                .addSecurityItem(securityRequirement)
                .components(components);

        // 서버 URL 설정
        List<Server> servers = new ArrayList<>();

        if ("local".equals(profile) || "dev".equals(profile)) {
            // 로컬 개발 환경
            Server localServer = new Server();
            localServer.setUrl("http://localhost:" + serverPort);
            localServer.setDescription("Local Development Server");
            servers.add(localServer);

            // 로컬에서도 HTTPS 테스트 가능하도록
            Server localHttpsServer = new Server();
            localHttpsServer.setUrl("https://localhost:" + serverPort);
            localHttpsServer.setDescription("Local HTTPS Server");
            servers.add(localHttpsServer);
        } else {
            // 운영 환경 - HTTPS만 사용
            Server prodServer = new Server();

            if (swaggerServerUrl != null && !swaggerServerUrl.trim().isEmpty()) {
                prodServer.setUrl(swaggerServerUrl);
            } else {
                prodServer.setUrl("https://api.interv.swote.dev");
            }
            prodServer.setDescription("Production Server (HTTPS)");
            servers.add(prodServer);

            // 백업 서버 URL (필요 시)
            Server backupServer = new Server();
            backupServer.setUrl("https://api-backup.interv.swote.dev");
            backupServer.setDescription("Backup Production Server");
            servers.add(backupServer);
        }

        openAPI.setServers(servers);

        return openAPI;
    }
}