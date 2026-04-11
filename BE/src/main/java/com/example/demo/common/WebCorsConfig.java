package com.example.demo.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * CORS configuration for Spring MVC.
 * Note: CORS is primarily handled by SecurityConfig via CorsConfigurationSource.
 * This config is kept for backward compatibility but may not be used when
 * Spring Security CORS handling is active.
 */
@Configuration
public class WebCorsConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173,http://localhost:4173,http://127.0.0.1:4173}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins(parseAllowedOrigins())
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD")
            .allowedHeaders(
                "Authorization",
                "Content-Type",
                "Accept",
                "Accept-Language",
                "Origin",
                "X-Requested-With",
                "Cache-Control",
                "Pragma"
            )
            .allowCredentials(true)
            .maxAge(3600);
    }

    private String[] parseAllowedOrigins() {
        return Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(origin -> !origin.isEmpty())
            .toArray(String[]::new);
    }
}
