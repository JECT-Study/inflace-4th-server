package com.example.inflace.global.interceptor;

import com.example.inflace.domain.idempotency.model.IdempotencyKeyMetadata;
import com.example.inflace.domain.idempotency.service.IdempotencyService;
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdempotencyKeyInterceptorTest {

    private IdempotencyService idempotencyService;
    private IdempotencyKeyInterceptor interceptor;

    @BeforeEach
    void setUp() {
        idempotencyService = mock(IdempotencyService.class);
        interceptor = new IdempotencyKeyInterceptor(idempotencyService);
    }

    @Test
    void 멱등성_키가_있는_변경_요청은_키를_저장하고_진행한다() throws Exception {
        MockHttpServletRequest request = request("POST", "/api/v1/channels", "key-1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(idempotencyService.saveIfAbsent(eq("key-1"), any(IdempotencyKeyMetadata.class)))
                .thenReturn(true);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result)
                .as("expected a newly saved idempotency request to proceed but interceptor blocked it")
                .isTrue();
        verify(idempotencyService).saveIfAbsent(eq("key-1"), any(IdempotencyKeyMetadata.class));
    }

    @Test
    void 중복_멱등성_요청은_GlobalExceptionHandler가_처리할_ApiException을_던진다() {
        MockHttpServletRequest request = request("DELETE", "/api/v1/channels/1", "key-1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(idempotencyService.saveIfAbsent(eq("key-1"), any(IdempotencyKeyMetadata.class)))
                .thenReturn(false);

        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                .as("expected a duplicate idempotency request to throw ApiException")
                .isInstanceOf(ApiException.class)
                .extracting("error")
                .as("expected duplicate idempotency error but got a different error")
                .isEqualTo(ErrorDefine.DUPLICATE_IDEMPOTENCY_REQUEST);
    }

    @Test
    void 대상_GET_요청은_멱등성_키를_저장한다() throws Exception {
        MockHttpServletRequest request = request(
                "GET",
                "/api/v1/influencers/123/insight-summary",
                "key-1"
        );
        when(idempotencyService.saveIfAbsent(eq("key-1"), any(IdempotencyKeyMetadata.class)))
                .thenReturn(true);

        boolean result = interceptor.preHandle(
                request,
                new MockHttpServletResponse(),
                new Object()
        );

        assertThat(result)
                .as("expected a target GET request with a new idempotency key to proceed")
                .isTrue();
        verify(idempotencyService).saveIfAbsent(eq("key-1"), any(IdempotencyKeyMetadata.class));
    }

    @Test
    void 일반_GET_요청은_멱등성_검사를_하지_않는다() throws Exception {
        MockHttpServletRequest request = request("GET", "/api/v1/channels", "key-1");

        boolean result = interceptor.preHandle(
                request,
                new MockHttpServletResponse(),
                new Object()
        );

        assertThat(result)
                .as("expected a non-target GET request to skip idempotency checking")
                .isTrue();
        verify(idempotencyService, never()).saveIfAbsent(any(), any());
    }

    @Test
    void 키가_없거나_허용된_경로인_요청은_멱등성_검사를_하지_않는다() throws Exception {
        MockHttpServletRequest requestWithoutKey = request("POST", "/api/v1/channels", null);
        MockHttpServletRequest allowedRequest = request("POST", "/api/v1/auth/login", "key-1");

        boolean withoutKeyResult = interceptor.preHandle(
                requestWithoutKey,
                new MockHttpServletResponse(),
                new Object()
        );
        boolean allowedPathResult = interceptor.preHandle(
                allowedRequest,
                new MockHttpServletResponse(),
                new Object()
        );

        assertThat(withoutKeyResult)
                .as("expected a request without an idempotency key to skip idempotency checking")
                .isTrue();
        assertThat(allowedPathResult)
                .as("expected an allowed-path request to skip idempotency checking")
                .isTrue();
        verify(idempotencyService, never()).saveIfAbsent(any(), any());
    }

    private MockHttpServletRequest request(String method, String path, String idempotencyKey) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        if (idempotencyKey != null) {
            request.addHeader("Idempotency-Key", idempotencyKey);
        }
        return request;
    }
}
