package com.example.inflace.global.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

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
}
