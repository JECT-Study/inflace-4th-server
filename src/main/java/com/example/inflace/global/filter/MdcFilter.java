package com.example.inflace.global.filter;

import com.example.inflace.global.config.AuthUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
public class MdcFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            MDC.put("coId", "req-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
            MDC.put("clientIp", resolveClientIp(request));
            MDC.put("method", request.getMethod());
            MDC.put("url", request.getRequestURI());
            MDC.put("userId", resolveUserId());

            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }

    private String resolveUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return "인증 불필요 요청";
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthUser authUser) {
            return authUser.userId().toString();
        }

        return "인증 불필요 요청";
    }
}
