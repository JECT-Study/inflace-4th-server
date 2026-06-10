package com.example.inflace.global.aop;

import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import com.example.inflace.global.notification.discord.DiscordErrorNotificationService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageConversionException;

import java.util.concurrent.CompletionException;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ErrorNotificationAspectTest {

    private final DiscordErrorNotificationService notificationService = mock(DiscordErrorNotificationService.class);
    private final ErrorNotificationAspect aspect = new ErrorNotificationAspect(notificationService);

    @Test
    void notifyControllerException_delegatesUnexpectedExceptionAsServerError() {
        JoinPoint joinPoint = joinPoint(TestController.class, "getChannel");
        IllegalStateException exception = new IllegalStateException("boom");

        aspect.notifyControllerException(joinPoint, exception);

        verify(notificationService).notifyAsync(
                "TestController.getChannel",
                exception,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "500",
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase()
        );
    }

    @Test
    void notifyControllerException_unwrapsCompletionApiException() {
        JoinPoint joinPoint = joinPoint(TestController.class, "getChannel");
        ApiException apiException = new ApiException(ErrorDefine.PROFILE_IMAGE_UPLOAD_FAILED);

        aspect.notifyControllerException(joinPoint, new CompletionException(apiException));

        verify(notificationService).notifyAsync(
                "TestController.getChannel",
                apiException,
                ErrorDefine.PROFILE_IMAGE_UPLOAD_FAILED.getHttpStatus(),
                ErrorDefine.PROFILE_IMAGE_UPLOAD_FAILED.getErrorCode(),
                ErrorDefine.PROFILE_IMAGE_UPLOAD_FAILED.getMessage()
        );
    }

    @Test
    void notifyFilterError_delegatesFilterError() {
        JoinPoint joinPoint = joinPoint(
                com.example.inflace.global.response.ApiFilterErrorResponseWriter.class,
                "write",
                null,
                null,
                ErrorDefine.DUPLICATE_IDEMPOTENCY_REQUEST
        );

        aspect.notifyFilterError(joinPoint, ErrorDefine.DUPLICATE_IDEMPOTENCY_REQUEST);

        verify(notificationService).notifyAsync(
                org.mockito.ArgumentMatchers.eq("ApiFilterErrorResponseWriter.write"),
                org.mockito.ArgumentMatchers.any(ApiException.class),
                org.mockito.ArgumentMatchers.eq(ErrorDefine.DUPLICATE_IDEMPOTENCY_REQUEST.getHttpStatus()),
                org.mockito.ArgumentMatchers.eq(ErrorDefine.DUPLICATE_IDEMPOTENCY_REQUEST.getErrorCode()),
                org.mockito.ArgumentMatchers.eq(ErrorDefine.DUPLICATE_IDEMPOTENCY_REQUEST.getMessage())
        );
    }

    @Test
    void notifyControllerException_skipsOrdinaryClientError() {
        JoinPoint joinPoint = joinPoint(TestController.class, "getChannel");

        aspect.notifyControllerException(joinPoint, new ApiException(ErrorDefine.USER_NOT_FOUND));

        verify(notificationService, never()).notifyAsync(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void notifyControllerException_skipsMessageConversionError() {
        JoinPoint joinPoint = joinPoint(TestController.class, "getChannel");

        aspect.notifyControllerException(joinPoint, new HttpMessageConversionException("invalid"));

        verify(notificationService, never()).notifyAsync(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void notifyFilterError_skipsOrdinaryClientError() {
        JoinPoint joinPoint = joinPoint(
                com.example.inflace.global.response.ApiFilterErrorResponseWriter.class,
                "write",
                null,
                null,
                ErrorDefine.AUTHENTICATION_FAILED
        );

        aspect.notifyFilterError(joinPoint, ErrorDefine.AUTHENTICATION_FAILED);

        verify(notificationService, never()).notifyAsync(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    private JoinPoint joinPoint(Class<?> declaringType, String methodName, Object... arguments) {
        JoinPoint joinPoint = mock(JoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        given(joinPoint.getSignature()).willReturn(signature);
        given(joinPoint.getArgs()).willReturn(arguments);
        given(signature.getDeclaringType()).willReturn(declaringType);
        given(signature.getName()).willReturn(methodName);
        return joinPoint;
    }

    private static final class TestController {
    }
}
