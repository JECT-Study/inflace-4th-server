package com.example.inflace.global.security.jwt;

import com.example.inflace.domain.auth.service.AuthTokenRedisService;
import com.example.inflace.domain.user.domain.enums.Plan;
import com.example.inflace.domain.user.infra.UserReadRepository;
import com.example.inflace.global.exception.ErrorDefine;
import com.example.inflace.global.security.custom.CustomAuthenticationEntryPoint;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private JwtProvider jwtProvider;
    private AuthTokenRedisService authTokenRedisService;
    private UserReadRepository userReadRepository;
    private CustomAuthenticationEntryPoint authenticationEntryPoint;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        jwtProvider = mock(JwtProvider.class);
        authTokenRedisService = mock(AuthTokenRedisService.class);
        userReadRepository = mock(UserReadRepository.class);
        authenticationEntryPoint = mock(CustomAuthenticationEntryPoint.class);
        filter = new JwtAuthenticationFilter(
                jwtProvider,
                authTokenRedisService,
                userReadRepository,
                authenticationEntryPoint
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesWhenTokenPlanMatchesCurrentUserPlan() throws Exception {
        UUID userId = UUID.randomUUID();
        MockHttpServletRequest request = authenticatedRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        stubToken(userId, Plan.ADMIN);
        when(userReadRepository.findPlanByUserId(userId)).thenReturn(Optional.of(Plan.ADMIN));

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .as("expected authentication to contain only the verified plan authority")
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
        verify(jwtProvider, never()).getUserRoles(anyString());
        verify(chain).doFilter(request, response);
    }

    @Test
    void rejectsTokenWhenPlanDoesNotMatchCurrentUserPlan() throws Exception {
        UUID userId = UUID.randomUUID();
        MockHttpServletRequest request = authenticatedRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        stubToken(userId, Plan.ADMIN);
        when(userReadRepository.findPlanByUserId(userId)).thenReturn(Optional.of(Plan.FREE));

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .as("expected mismatched plan token not to authenticate")
                .isNull();
        verify(authenticationEntryPoint).commence(
                org.mockito.ArgumentMatchers.eq(request),
                org.mockito.ArgumentMatchers.eq(response),
                argThat(exception -> exception instanceof JwtAuthenticationException jwtException
                        && jwtException.getError() == ErrorDefine.INVALID_ACCESS_TOKEN)
        );
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void rejectsTokenWithoutPlanClaim() throws Exception {
        UUID userId = UUID.randomUUID();
        MockHttpServletRequest request = authenticatedRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        stubToken(userId, null);
        when(userReadRepository.findPlanByUserId(userId)).thenReturn(Optional.of(Plan.FREE));

        filter.doFilterInternal(request, response, chain);

        verify(authenticationEntryPoint).commence(
                org.mockito.ArgumentMatchers.eq(request),
                org.mockito.ArgumentMatchers.eq(response),
                argThat(exception -> exception instanceof JwtAuthenticationException jwtException
                        && jwtException.getError() == ErrorDefine.INVALID_ACCESS_TOKEN)
        );
        verify(chain, never()).doFilter(request, response);
    }

    private MockHttpServletRequest authenticatedRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
        request.setServletPath("/api/v1/users/me");
        request.addHeader("Authorization", "Bearer access-token");
        return request;
    }

    private void stubToken(UUID userId, Plan plan) {
        when(authTokenRedisService.isLogoutAccessToken("access-token")).thenReturn(false);
        when(jwtProvider.isValid("access-token")).thenReturn(true);
        when(jwtProvider.getUserId("access-token")).thenReturn(userId);
        when(jwtProvider.getPlan("access-token")).thenReturn(plan);
    }
}
