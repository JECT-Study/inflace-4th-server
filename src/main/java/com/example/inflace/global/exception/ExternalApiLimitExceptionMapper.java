package com.example.inflace.global.exception;

import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;

public final class ExternalApiLimitExceptionMapper {

    private ExternalApiLimitExceptionMapper() {
    }

    public static RuntimeException toRuntimeException(Throwable throwable) {
        if (throwable instanceof BulkheadFullException || throwable instanceof RequestNotPermitted) {
            return new ApiException(ErrorDefine.EXTERNAL_API_RATE_LIMITED);
        }
        if (throwable instanceof ApiException apiException) {
            return apiException;
        }
        if (throwable instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new RuntimeException(throwable);
    }
}
