package com.example.inflace.global.security.jwt;

import com.example.inflace.domain.auth.service.AuthTokenRedisService;
import com.example.inflace.domain.user.domain.enums.Plan;
import com.example.inflace.domain.user.infra.UserReadRepository;
import com.example.inflace.global.config.AuthUser;
import com.example.inflace.global.exception.ErrorDefine;
import com.example.inflace.global.security.config.SecurityAllowedPaths;
import com.example.inflace.global.security.custom.CustomAuthenticationEntryPoint;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.slf4j.MDC;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final AuthTokenRedisService authTokenRedisService;
    private final UserReadRepository userReadRepository;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String requestPath = request.getServletPath();
        return Arrays.stream(SecurityAllowedPaths.allowedPaths())
                .anyMatch(path -> PATH_MATCHER.match(path, requestPath));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            if (authTokenRedisService.isLogoutAccessToken(token)) {
                throw new JwtAuthenticationException(ErrorDefine.LOGGED_OUT_ACCESS_TOKEN);
            }
            if (!jwtProvider.isValid(token)) {
                throw new JwtAuthenticationException(ErrorDefine.INVALID_ACCESS_TOKEN);
            }

            UUID userId = jwtProvider.getUserId(token);
            Plan plan = jwtProvider.getPlan(token);
            validateUserPlan(userId, plan);
            List<GrantedAuthority> authorities = buildAuthorities(plan);
            AuthUser authUser = new AuthUser(userId);
            PreAuthenticatedAuthenticationToken authentication =
                    new PreAuthenticatedAuthenticationToken(authUser, token, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            MDC.put("userId", userId.toString());
        } catch (JwtAuthenticationException e) {
            SecurityContextHolder.clearContext();
            customAuthenticationEntryPoint.commence(request, response, e);
            return;
        } catch (RuntimeException e) {
            SecurityContextHolder.clearContext();
            customAuthenticationEntryPoint.commence(
                    request,
                    response,
                    new JwtAuthenticationException(ErrorDefine.AUTHENTICATION_FAILED, null, e)
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private List<GrantedAuthority> buildAuthorities(Plan plan) {
        return List.of(new SimpleGrantedAuthority(plan.toSpringRole()));
    }

    private void validateUserPlan(UUID userId, Plan tokenPlan) {
        Plan currentPlan = userReadRepository.findPlanByUserId(userId)
                .orElseThrow(() -> new JwtAuthenticationException(ErrorDefine.USER_NOT_FOUND));
        if (tokenPlan == null || currentPlan != tokenPlan) {
            throw new JwtAuthenticationException(ErrorDefine.INVALID_ACCESS_TOKEN);
        }
    }
}
