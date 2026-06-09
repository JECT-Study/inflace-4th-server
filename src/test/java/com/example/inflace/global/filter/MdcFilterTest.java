package com.example.inflace.global.filter;

import com.example.inflace.global.config.AuthUser;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class MdcFilterTest {

    private final MdcFilter mdcFilter = new MdcFilter();

    @AfterEach
    void tearDown() {
        MDC.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void 인증된_요청에서는_MDC_값을_채우고_요청_후_비운다() throws Exception {
        UUID userId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        SecurityContextHolder.getContext().setAuthentication(
                new PreAuthenticatedAuthenticationToken(
                        new AuthUser(userId),
                        "token",
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                )
        );

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/channels");
        request.addHeader("X-Forwarded-For", "203.0.113.7, 10.0.0.1");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> coId = new AtomicReference<>();
        AtomicReference<String> clientIp = new AtomicReference<>();
        AtomicReference<String> method = new AtomicReference<>();
        AtomicReference<String> url = new AtomicReference<>();
        AtomicReference<String> mdcUserId = new AtomicReference<>();

        FilterChain chain = (req, res) -> {
            coId.set(MDC.get("coId"));
            clientIp.set(MDC.get("clientIp"));
            method.set(MDC.get("method"));
            url.set(MDC.get("url"));
            mdcUserId.set(MDC.get("userId"));
        };

        mdcFilter.doFilter(request, response, chain);

        assertThat(coId.get()).startsWith("req-");
        assertThat(clientIp.get()).isEqualTo("203.0.113.7");
        assertThat(method.get()).isEqualTo("GET");
        assertThat(url.get()).isEqualTo("/api/v1/channels");
        assertThat(mdcUserId.get()).isEqualTo(userId.toString());
        assertThat(MDC.get("coId")).isNull();
    }

    @Test
    void 인증이_없으면_userId는_기본_문구를_쓴다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/channels");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> userId = new AtomicReference<>();

        FilterChain chain = (req, res) -> userId.set(MDC.get("userId"));

        mdcFilter.doFilter(request, response, chain);

        assertThat(userId.get()).isEqualTo("인증 불필요 요청");
        assertThat(MDC.get("userId")).isNull();
    }
}
