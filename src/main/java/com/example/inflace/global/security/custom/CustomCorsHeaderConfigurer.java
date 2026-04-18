package com.example.inflace.global.security.custom;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;

import java.util.List;

public final class CustomCorsHeaderConfigurer {

    private CustomCorsHeaderConfigurer() {
    }

    public static void setCorsHeader(
            HttpServletRequest request,
            HttpServletResponse response,
            List<String> allowedOrigins
    ) {
        String requestOrigin = request.getHeader(HttpHeaders.ORIGIN);

        if (requestOrigin != null && allowedOrigins != null && allowedOrigins.contains(requestOrigin)) {
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, requestOrigin);
            response.setHeader(HttpHeaders.VARY, HttpHeaders.ORIGIN);
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        }

        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, PATCH, DELETE, OPTIONS");
        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "Authorization, Content-Type, X-Requested-With");
    }
}
