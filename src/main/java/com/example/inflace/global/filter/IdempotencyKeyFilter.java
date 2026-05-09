package com.example.inflace.global.filter;

import com.example.inflace.domain.idempotency.model.IdempotencyKeyMetadata;
import com.example.inflace.domain.idempotency.service.IdempotencyService;
import com.example.inflace.global.exception.ErrorDefine;
import com.example.inflace.global.response.ApiFilterErrorResponseWriter;
import com.example.inflace.global.security.config.SecurityAllowedPaths;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;

@Slf4j
@RequiredArgsConstructor
public class IdempotencyKeyFilter extends OncePerRequestFilter {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final IdempotencyService idempotencyService;
    private final ApiFilterErrorResponseWriter apiFilterErrorResponseWriter;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        if (!shouldFilterByMethodOrPath(request)) {
            return true;
        }

        String idempotencyKey = resolveIdempotencyKey(request);
        if (!StringUtils.hasText(idempotencyKey)) {
            return true;
        }

        String requestPath = request.getServletPath();
        return Arrays.stream(SecurityAllowedPaths.allowedPaths())
                .anyMatch(path -> PATH_MATCHER.match(path, requestPath));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String idempotencyKey = resolveIdempotencyKey(request);
        IdempotencyKeyMetadata metadata = new IdempotencyKeyMetadata(
                request.getMethod(),
                request.getRequestURI(),
                LocalDateTime.now()
        );

        boolean saved = idempotencyService.saveIfAbsent(idempotencyKey, metadata);
        if (!saved) {
            log.warn(
                    "Duplicate idempotency request blocked. key={}, method={}, path={}",
                    idempotencyKey,
                    metadata.method(),
                    metadata.path()
            );
            apiFilterErrorResponseWriter.write(
                    request,
                    response,
                    ErrorDefine.DUPLICATE_IDEMPOTENCY_REQUEST,
                    ErrorDefine.DUPLICATE_IDEMPOTENCY_REQUEST.getMessage()
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveIdempotencyKey(HttpServletRequest request) {
        return request.getHeader(IDEMPOTENCY_KEY_HEADER);
    }

    private boolean shouldFilterByMethodOrPath(HttpServletRequest request) {
        return isMutationMethod(request.getMethod())
                || isInsightSummaryRequest(request);
    }

    private boolean isMutationMethod(String method) {
        return HttpMethod.POST.matches(method)
                || HttpMethod.PUT.matches(method)
                || HttpMethod.PATCH.matches(method)
                || HttpMethod.DELETE.matches(method);
    }

    private boolean isInsightSummaryRequest(HttpServletRequest request) {
        if (!HttpMethod.GET.matches(request.getMethod())) {
            return false;
        }

        String requestPath = request.getServletPath();
        return Arrays.stream(IdempotencyTargetPaths.targetGetPaths())
                .anyMatch(path -> PATH_MATCHER.match(path, requestPath));
    }
}
