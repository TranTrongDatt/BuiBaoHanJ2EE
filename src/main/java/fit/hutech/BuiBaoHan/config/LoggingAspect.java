package fit.hutech.BuiBaoHan.config;

import java.util.Arrays;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * AOP-based logging aspect for cross-cutting logging concerns
 */
@Aspect
@Component
@Slf4j
public class LoggingAspect {

    /**
     * Pointcut for all controllers
     */
    @Pointcut("within(fit.hutech.BuiBaoHan.controllers..*)")
    public void controllerPointcut() {}

    /**
     * Pointcut for all services
     */
    @Pointcut("within(fit.hutech.BuiBaoHan.services..*)")
    public void servicePointcut() {}

    /**
     * Pointcut for all repositories
     */
    @Pointcut("within(fit.hutech.BuiBaoHan.repositories..*)")
    public void repositoryPointcut() {}

    /**
     * Log method execution for controllers
     */
    @Around("controllerPointcut()")
    public Object logControllerExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        
        log.info("==> {}.{}() with arguments: {}", 
                className, methodName, Arrays.toString(joinPoint.getArgs()));
        
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed();
            long elapsedTime = System.currentTimeMillis() - startTime;
            
            log.info("<== {}.{}() completed in {}ms", className, methodName, elapsedTime);
            
            return result;
        } catch (Exception e) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.error("<== {}.{}() threw {} after {}ms", 
                    className, methodName, e.getClass().getSimpleName(), elapsedTime);
            throw e;
        }
    }

    /**
     * Log method execution for services (debug level)
     */
    @Around("servicePointcut()")
    public Object logServiceExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!log.isDebugEnabled()) {
            return joinPoint.proceed();
        }
        
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        
        log.debug("==> {}.{}()", className, methodName);
        
        long startTime = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long elapsedTime = System.currentTimeMillis() - startTime;
        
        log.debug("<== {}.{}() returned in {}ms", className, methodName, elapsedTime);
        
        return result;
    }

    /**
     * Log exceptions thrown in controllers
     */
    @AfterThrowing(pointcut = "controllerPointcut()", throwing = "ex")
    public void logControllerException(JoinPoint joinPoint, Throwable ex) {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        
        log.error("Exception in {}.{}(): {} - {}", 
                className, methodName, ex.getClass().getSimpleName(), ex.getMessage());
    }

    /**
     * Log exceptions thrown in services
     */
    @AfterThrowing(pointcut = "servicePointcut()", throwing = "ex")
    public void logServiceException(JoinPoint joinPoint, Throwable ex) {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        
        log.error("Service exception in {}.{}(): {} - {}", 
                className, methodName, ex.getClass().getSimpleName(), ex.getMessage());
        
        if (log.isDebugEnabled()) {
            log.debug("Full stack trace:", ex);
        }
    }

    /**
     * Log slow queries (> 1000ms)
     */
    @Around("repositoryPointcut()")
    public Object logSlowQueries(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long elapsedTime = System.currentTimeMillis() - startTime;
        
        if (elapsedTime > 1000) {
            String className = joinPoint.getSignature().getDeclaringTypeName();
            String methodName = joinPoint.getSignature().getName();
            
            log.warn("SLOW QUERY: {}.{}() took {}ms", className, methodName, elapsedTime);
        }
        
        return result;
    }
}
