package com.example.inflace.global.config;

import com.example.inflace.global.properties.CorsAllowedOriginsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final CorsAllowedOriginsProperties corsAllowedOriginsProperties;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> origins = corsAllowedOriginsProperties.getOrigins();
        if (origins == null || origins.isEmpty()) {
            throw new IllegalStateException("cors.allowed.origins must not be empty");
        }

        registry.addMapping("/**")
                .allowedMethods("*")
                .allowedHeaders("*")
                .exposedHeaders(HttpHeaders.AUTHORIZATION)
                .allowedOrigins(origins.toArray(String[]::new))
                .allowCredentials(true);
    }
}
