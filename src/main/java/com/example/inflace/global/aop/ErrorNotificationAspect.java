package com.example.inflace.global.aop;

import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import com.example.inflace.global.notification.discord.DiscordErrorNotificationService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.EnumSet;
import java.util.concurrent.CompletionException;

@Aspect
@Component
@RequiredArgsConstructor
public class ErrorNotificationAspect {

    private static final EnumSet<ErrorDefine> NOTIFIABLE_CLIENT_ERRORS = EnumSet.of(
            ErrorDefine.LOGGED_OUT_ACCESS_TOKEN,
            ErrorDefine.DUPLICATE_IDEMPOTENCY_REQUEST
    );

    private final DiscordErrorNotificationService discordErrorNotificationService;

    @AfterThrowing(
            pointcut = "within(@org.springframework.web.bind.annotation.RestController *)",
            throwing = "exception"
    )
    public void notifyControllerException(JoinPoint joinPoint, Throwable exception) {
        Throwable notificationTarget = unwrapCompletionException(exception);
        if (!shouldNotify(notificationTarget)) {
            return;
        }

        String source = resolveSource(joinPoint);

        if (notificationTarget instanceof ApiException apiException) {
            ErrorDefine errorDefine = apiException.getError();
            notify(source, apiException, errorDefine, errorDefine.getMessage());
            return;
        }

        discordErrorNotificationService.notifyAsync(
                source,
                notificationTarget,
                HttpStatus.INTERNAL_SERVER_ERROR,
                Integer.toString(HttpStatus.INTERNAL_SERVER_ERROR.value()),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase()
        );
    }

    @AfterReturning(
            value = "execution(* com.example.inflace.global.response.ApiFilterErrorResponseWriter.write(..))"
                    + " && args(*, *, errorDefine)",
            argNames = "errorDefine"
    )
    public void notifyFilterError(JoinPoint joinPoint, ErrorDefine errorDefine) {
        if (!shouldNotify(errorDefine)) {
            return;
        }

        notify(resolveSource(joinPoint), new ApiException(errorDefine), errorDefine, errorDefine.getMessage());
    }

    private boolean shouldNotify(Throwable exception) {
        if (exception instanceof MethodArgumentNotValidException
                || exception instanceof HttpMessageConversionException) {
            return false;
        }

        if (exception instanceof ApiException apiException) {
            return shouldNotify(apiException.getError());
        }

        return true;
    }

    private boolean shouldNotify(ErrorDefine errorDefine) {
        return errorDefine.getHttpStatus().is5xxServerError()
                || NOTIFIABLE_CLIENT_ERRORS.contains(errorDefine);
    }

    private void notify(String source, Throwable exception, ErrorDefine errorDefine, String message) {
        discordErrorNotificationService.notifyAsync(
                source,
                exception,
                errorDefine.getHttpStatus(),
                errorDefine.getErrorCode(),
                message
        );
    }

    private Throwable unwrapCompletionException(Throwable exception) {
        if (exception instanceof CompletionException completionException
                && completionException.getCause() != null) {
            return completionException.getCause();
        }

        return exception;
    }

    private String resolveSource(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getDeclaringType().getSimpleName() + "." + signature.getName();
    }
}
