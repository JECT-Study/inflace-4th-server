package com.example.inflace.global.exception;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.junit.jupiter.api.Test;

class ExternalApiLimitExceptionMapperTest {

    @Test
    void bulkheadFullException_mapsToExternalApiRateLimited() {
        RuntimeException result = ExternalApiLimitExceptionMapper.toRuntimeException(
                BulkheadFullException.createBulkheadFullException(Bulkhead.ofDefaults("youtube-analytics"))
        );

        assertThat(result)
                .as("expected bulkhead permit exhaustion to be exposed as external API 429")
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getError()).isEqualTo(ErrorDefine.EXTERNAL_API_RATE_LIMITED));
    }

    @Test
    void requestNotPermitted_mapsToExternalApiRateLimited() {
        RuntimeException result = ExternalApiLimitExceptionMapper.toRuntimeException(
                RequestNotPermitted.createRequestNotPermitted(RateLimiter.ofDefaults("youtube-analytics"))
        );

        assertThat(result)
                .as("expected rate limiter permit exhaustion to be exposed as external API 429")
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getError()).isEqualTo(ErrorDefine.EXTERNAL_API_RATE_LIMITED));
    }

    @Test
    void apiException_isPreserved() {
        ApiException source = new ApiException(ErrorDefine.YOUTUBE_API_ERROR);

        RuntimeException result = ExternalApiLimitExceptionMapper.toRuntimeException(source);

        assertThat(result)
                .as("expected existing ApiException to keep its original error definition")
                .isSameAs(source);
    }

    @Test
    void runtimeException_isPreserved() {
        IllegalStateException source = new IllegalStateException("unexpected failure");

        RuntimeException result = ExternalApiLimitExceptionMapper.toRuntimeException(source);

        assertThat(result)
                .as("expected non-limit runtime exception not to be mislabeled as 429")
                .isSameAs(source);
    }
}
