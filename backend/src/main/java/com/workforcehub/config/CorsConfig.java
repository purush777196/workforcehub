package com.workforcehub.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * Cross-Origin Resource Sharing (CORS) Configuration.
 * Binds development origin (http://localhost:5173) to API routes safely.
 * Never uses wildcard (*) origins with allowCredentials=true.
 */
@Configuration
public class CorsConfig {

    @Value("${workforcehub.cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    @Value("${workforcehub.cors.allowed-methods:GET,POST,PUT,PATCH,DELETE,OPTIONS}")
    private String allowedMethods;

    @Value("${workforcehub.cors.allowed-headers:*}")
    private String allowedHeaders;

    @Value("${workforcehub.cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Value("${workforcehub.cors.max-age:3600}")
    private long maxAge;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        config.setAllowedOrigins(origins);

        List<String> methods = Arrays.stream(allowedMethods.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        config.setAllowedMethods(methods);

        List<String> headers = Arrays.stream(allowedHeaders.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        config.setAllowedHeaders(headers);

        config.setAllowCredentials(allowCredentials);
        config.setMaxAge(maxAge);

        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public CorsFilter corsFilter() {
        return new CorsFilter(corsConfigurationSource());
    }
}
