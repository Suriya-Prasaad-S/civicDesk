package com.civicdesk.grievance.logging;

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

    @Pointcut("execution(* com.civicdesk.grievance..*(..))")
    public void applicationPackagePointcut() {
        // All classes inside grievance service
    }

    @Pointcut("!within(com.civicdesk.grievance.logging..*)")
    public void excludeLoggingPackagePointcut() {
        // Prevent aspect from logging itself
    }

    @Pointcut("!within(com.civicdesk.grievance.security..*)")
    public void excludeSecurityPackagePointcut() {
        // Exclude security classes
    }

    @Around("applicationPackagePointcut() && excludeLoggingPackagePointcut() && excludeSecurityPackagePointcut()")
    public Object logExecution(ProceedingJoinPoint joinPoint) throws Throwable {

        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();

        log.info("START -> {}.{}()", className, methodName);

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();

            long timeTaken = System.currentTimeMillis() - startTime;

            log.info("END -> {}.{}() | Time: {} ms",
                    className,
                    methodName,
                    timeTaken);

            return result;

        } catch (Exception ex) {

            log.error("ERROR -> {}.{}()",
                    className,
                    methodName,
                    ex);

            throw ex;
        }
    }
}