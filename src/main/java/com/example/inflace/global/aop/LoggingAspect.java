package com.example.inflace.global.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Around("""
            within(@org.springframework.web.bind.annotation.RestController *)
            && !within(com.example.inflace.global.controller.HealthCheckController)
            """)
    public Object logControllerExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        return logExecution(joinPoint, "controller");
    }

    @Around("""
            within(@org.springframework.stereotype.Service *)
            && !within(com.example.inflace..*RedisService)
            """)
    public Object logServiceExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        return logExecution(joinPoint, "service");
    }

    @AfterThrowing(
            pointcut = "within(@org.springframework.stereotype.Service *)",
            throwing = "exception"
    )
    public void logServiceException(JoinPoint joinPoint, Throwable exception) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Map<String, String> mdcInfo = getMdcInfo();

        log.error(
                "Service exception: {}.{}() - mdc={}, exceptionType={}, message={}",
                signature.getDeclaringType().getSimpleName(),
                signature.getName(),
                mdcInfo,
                exception.getClass().getName(),
                exception.getMessage(),
                exception
        );
    }

    private Object logExecution(ProceedingJoinPoint joinPoint, String layer) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();
        Map<String, String> mdcInfo = getMdcInfo();

        log.info("{} started: {}.{}() - mdc={}", layer, className, methodName, mdcInfo);

        long startTime = System.currentTimeMillis();
        try {
            return joinPoint.proceed();
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            log.info(
                    "{} completed: {}.{}() - executionTime={}ms - mdc={}",
                    layer,
                    className,
                    methodName,
                    executionTime,
                    mdcInfo
            );
        }
    }

    private Map<String, String> getMdcInfo() {
        Map<String, String> mdcInfo = new LinkedHashMap<>();
        mdcInfo.put("coId", valueOrBlank(MDC.get("coId")));
        mdcInfo.put("clientIp", valueOrBlank(MDC.get("clientIp")));
        mdcInfo.put("method", valueOrBlank(MDC.get("method")));
        mdcInfo.put("url", valueOrBlank(MDC.get("url")));
        mdcInfo.put("userId", valueOrBlank(MDC.get("userId")));
        return mdcInfo;
    }

    private String valueOrBlank(String value) {
        return value != null ? value : "";
    }
}
