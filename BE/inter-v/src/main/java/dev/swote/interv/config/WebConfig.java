package dev.swote.interv.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc
@Slf4j
public class WebConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origins:}")
    private String allowedOrigins;

    @Value("${cors.allow-localhost:true}")
    private boolean allowLocalhost;

    @Value("${spring.profiles.active:local}")
    private String activeProfile;


    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(buildAllowedOrigins())
//                .allowedOrigins("*") //FOR DEV
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(-1)
                .allowCredentials(true)
                .maxAge(3000);
    }


    private String[] buildAllowedOrigins() {
        java.util.List<String> origins = new java.util.ArrayList<>();
        if (allowLocalhost || "local".equals(activeProfile) || "dev".equals(activeProfile)) {
            origins.addAll(java.util.Arrays.asList(
                    "http://localhost:*",
                    "http://127.0.0.1:*"
            ));
        }

        if (allowedOrigins != null && !allowedOrigins.trim().isEmpty()) {
            String[] additionalOrigins = allowedOrigins.split(",");
            for (String origin : additionalOrigins) {
                String trimmed = origin.trim();
                if (!trimmed.isEmpty() && !origins.contains(trimmed)) {
                    origins.add(trimmed);
                }
            }
        }

        if ("prod".equals(activeProfile)) {
            origins.addAll(java.util.Arrays.asList(
                    "https://app.interv.swote.dev",
                    "https://api.interv.swote.dev",
                    "https://ml.interv.swote.dev",
                    "https://interv.swote.dev",
                    "http://localhost:5173",
                    "http://localhost:3000",
                    "http://127.0.0.1:5173",
                    "http://127.0.0.1:3000"
            ));
        }

        return origins.toArray(new String[0]);
    }
}