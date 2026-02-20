package com.example.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CORS for frontends (Partner Portal :3000, Ops Dashboard :3001, Buyer Portal :3002)
 * calling the gateway at :8080. On GCP, set GATEWAY_PUBLIC_URL (e.g. http://34.116.185.179:8080)
 * so that origins for the same host (ports 80, 3000, 3001, 3002) are allowed.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        List<String> origins = new ArrayList<>(List.of(
                "http://localhost:3000",
                "http://localhost:3001",
                "http://localhost:3002"
        ));

        String publicUrl = System.getenv("GATEWAY_PUBLIC_URL");
        if (publicUrl != null && !publicUrl.isBlank()) {
            try {
                URI uri = URI.create(publicUrl.trim());
                String scheme = uri.getScheme();
                String host = uri.getHost();
                if (scheme != null && host != null) {
                    String base = scheme + "://" + host;
                    origins.add(base);
                    origins.add(base + ":80");
                    origins.add(base + ":3000");
                    origins.add(base + ":3001");
                    origins.add(base + ":3002");
                }
            } catch (Exception ignored) {
                // keep only localhost origins
            }
        }

        String extra = System.getenv("GATEWAY_CORS_ORIGINS");
        if (extra != null && !extra.isBlank()) {
            origins.addAll(Arrays.stream(extra.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList()));
        }

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
