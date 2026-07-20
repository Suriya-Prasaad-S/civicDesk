package com.civicdesk.analytics.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    /**
     * Trace only the meaningful boundaries — controllers and services. Repositories, DTOs, mappers,
     * config and security classes are intentionally excluded to keep the logs readable.
     */
    @Pointcut("execution(* com.civicdesk.analytics.controller..*(..)) "
            + "|| execution(* com.civicdesk.analytics.service..*(..))")
    public void controllerAndServicePointcut() {
        // Controller + service methods only.
    }

    @Around("controllerAndServicePointcut()")
    public Object logExecution(ProceedingJoinPoint joinPoint) throws Throwable {

        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        long startTime = System.currentTimeMillis();
        log.info("START -> {}.{}()", className, methodName);

        // Exceptions propagate untouched: GlobalExceptionHandler is the single owner of error
        // logging, so we don't log (and duplicate) the stack trace at every layer here.
        Object result = joinPoint.proceed();

        log.info("END   -> {}.{}() | Time: {} ms",
                className, methodName, System.currentTimeMillis() - startTime);
        return result;
    }
}
