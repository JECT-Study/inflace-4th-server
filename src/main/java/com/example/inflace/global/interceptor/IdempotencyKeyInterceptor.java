package com.example.inflace.global.interceptor;

import com.example.inflace.domain.idempotency.model.IdempotencyKeyMetadata;
import com.example.inflace.domain.idempotency.service.IdempotencyService;
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import com.example.inflace.global.security.config.SecurityAllowedPaths;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyKeyInterceptor implements HandlerInterceptor {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final String[] TARGET_GET_PATHS = {
            "/api/v1/influencers/*/insight-summary"
    };
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final IdempotencyService idempotencyService;

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws Exception {
        if (!shouldCheck(request)) {
            return true;
        }

        String idempotencyKey = request.getHeader(IDEMPOTENCY_KEY_HEADER);
        IdempotencyKeyMetadata metadata = new IdempotencyKeyMetadata(
                request.getMethod(),
                request.getRequestURI(),
                LocalDateTime.now()
        );

        if (idempotencyService.saveIfAbsent(idempotencyKey, metadata)) {
            return true;
        }

        log.warn(
                "Duplicate idempotency request blocked. key={}, method={}, path={}",
                idempotencyKey,
                metadata.method(),
                metadata.path()
        );
        throw new ApiException(ErrorDefine.DUPLICATE_IDEMPOTENCY_REQUEST);
    }

    private boolean shouldCheck(HttpServletRequest request) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())
                || !shouldCheckMethodOrPath(request)
                || !StringUtils.hasText(request.getHeader(IDEMPOTENCY_KEY_HEADER))) {
            return false;
        }

        String requestPath = request.getServletPath();
        return Arrays.stream(SecurityAllowedPaths.allowedPaths())
                .noneMatch(path -> PATH_MATCHER.match(path, requestPath));
    }

    private boolean shouldCheckMethodOrPath(HttpServletRequest request) {
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
        return HttpMethod.GET.matches(request.getMethod())
                && Arrays.stream(TARGET_GET_PATHS)
                .anyMatch(path -> PATH_MATCHER.match(path, request.getServletPath()));
    }
}
