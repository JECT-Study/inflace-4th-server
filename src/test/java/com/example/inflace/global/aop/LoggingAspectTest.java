package com.example.inflace.global.aop;

import com.example.inflace.domain.auth.service.AuthTokenRedisService;
import com.example.inflace.global.controller.HealthCheckController;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(OutputCaptureExtension.class)
class LoggingAspectTest {

    private final LoggingAspect loggingAspect = new LoggingAspect();

    @Test
    void 컨트롤러_실행의_시작과_종료를_기록한다(CapturedOutput output) throws Throwable {
        try {
            putMdc();
            ProceedingJoinPoint joinPoint = joinPoint("getChannel", "response");

            Object result = loggingAspect.logControllerExecution(joinPoint);

            assertThat(result).isEqualTo("response");
            assertThat(output)
                    .contains("controller started: TestTarget.getChannel() - mdc={coId=co-1, clientIp=127.0.0.1, method=GET, url=/api/v1/channels, userId=user-1}")
                    .contains("controller completed: TestTarget.getChannel() - executionTime=")
                    .contains("mdc={coId=co-1, clientIp=127.0.0.1, method=GET, url=/api/v1/channels, userId=user-1}")
                    .doesNotContain("secret-token");
        } finally {
            clearMdc();
        }
    }

    @Test
    void 서비스_실행의_시작과_종료를_기록한다(CapturedOutput output) throws Throwable {
        try {
            putMdc();
            ProceedingJoinPoint joinPoint = joinPoint("syncChannel", null);

            loggingAspect.logServiceExecution(joinPoint);

            assertThat(output)
                    .contains("service started: TestTarget.syncChannel() - mdc={coId=co-1, clientIp=127.0.0.1, method=GET, url=/api/v1/channels, userId=user-1}")
                    .contains("service completed: TestTarget.syncChannel() - executionTime=")
                    .contains("mdc={coId=co-1, clientIp=127.0.0.1, method=GET, url=/api/v1/channels, userId=user-1}");
        } finally {
            clearMdc();
        }
    }

    @Test
    void 서비스_예외의_타입과_메시지와_스택_트레이스를_기록한다(CapturedOutput output) {
        try {
            putMdc();
            ProceedingJoinPoint joinPoint = joinPoint("syncChannel", null);
            IllegalStateException exception = new IllegalStateException("sync failed");

            loggingAspect.logServiceException(joinPoint, exception);

            assertThat(output)
                    .contains("Service exception: TestTarget.syncChannel() - mdc={coId=co-1, clientIp=127.0.0.1, method=GET, url=/api/v1/channels, userId=user-1}")
                    .contains("exceptionType=java.lang.IllegalStateException")
                    .contains("message=sync failed")
                    .contains("java.lang.IllegalStateException: sync failed");
        } finally {
            clearMdc();
        }
    }

    @Test
    void 헬스_체크_컨트롤러는_로깅_대상에서_제외한다() throws NoSuchMethodException {
        assertThat(matches(
                aroundExpression("logControllerExecution"),
                HealthCheckController.class,
                "healthCheck",
                String.class
        )).isFalse();
    }

    @Test
    void 일반_컨트롤러는_로깅_대상에_포함한다() throws NoSuchMethodException {
        assertThat(matches(aroundExpression("logControllerExecution"), TestController.class, "getChannel"))
                .isTrue();
    }

    @Test
    void 레디스_서비스는_실행_로깅_대상에서_제외한다() throws NoSuchMethodException {
        assertThat(matches(
                aroundExpression("logServiceExecution"),
                AuthTokenRedisService.class,
                "saveLogoutAccessToken",
                String.class,
                long.class
        ))
                .isFalse();
    }

    @Test
    void 레디스_서비스는_예외_로깅_대상에_포함한다() throws NoSuchMethodException {
        assertThat(matches(
                afterThrowingExpression("logServiceException"),
                AuthTokenRedisService.class,
                "saveLogoutAccessToken",
                String.class,
                long.class
        ))
                .isTrue();
    }

    @Test
    void 일반_서비스는_로깅_대상에_포함한다() throws NoSuchMethodException {
        assertThat(matches(aroundExpression("logServiceExecution"), TestService.class, "syncChannel"))
                .isTrue();
    }

    private String aroundExpression(String methodName) throws NoSuchMethodException {
        return LoggingAspect.class
                .getMethod(methodName, ProceedingJoinPoint.class)
                .getAnnotation(Around.class)
                .value();
    }

    private String afterThrowingExpression(String methodName) throws NoSuchMethodException {
        return LoggingAspect.class
                .getMethod(methodName, org.aspectj.lang.JoinPoint.class, Throwable.class)
                .getAnnotation(AfterThrowing.class)
                .pointcut();
    }

    private boolean matches(String expression, Class<?> targetType, String methodName, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        pointcut.setExpression(expression);
        Method method = targetType.getMethod(methodName, parameterTypes);
        return pointcut.matches(method, targetType);
    }

    private ProceedingJoinPoint joinPoint(String methodName, Object result) {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);

        given(joinPoint.getSignature()).willReturn(signature);
        given(joinPoint.getArgs()).willReturn(new Object[]{"secret-token"});
        given(signature.getDeclaringType()).willReturn(TestTarget.class);
        given(signature.getName()).willReturn(methodName);
        try {
            given(joinPoint.proceed()).willReturn(result);
        } catch (Throwable throwable) {
            throw new IllegalStateException("expected join point setup but got exception", throwable);
        }

        return joinPoint;
    }

    private void putMdc() {
        MDC.put("coId", "co-1");
        MDC.put("clientIp", "127.0.0.1");
        MDC.put("method", "GET");
        MDC.put("url", "/api/v1/channels");
        MDC.put("userId", "user-1");
    }

    private void clearMdc() {
        MDC.clear();
    }

    private static final class TestTarget {
    }

    @RestController
    public static final class TestController {

        public void getChannel() {
        }
    }

    @Service
    public static final class TestService {

        public void syncChannel() {
        }
    }
}
