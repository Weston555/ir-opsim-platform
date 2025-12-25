package com.example.iropsim.audit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 审计日志AOP切面
 */
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLoggingAspect {

    private final AuditLogService auditLogService;

    @Pointcut("execution(* com.example.iropsim.controller.*.ackAlarm(..))")
    public void alarmAckMethods() {}

    @Pointcut("execution(* com.example.iropsim.controller.*.createKbCase(..))")
    public void kbCaseCreateMethods() {}

    @Pointcut("execution(* com.example.iropsim.controller.*.updateKbCase(..))")
    public void kbCaseUpdateMethods() {}

    @Pointcut("execution(* com.example.iropsim.controller.*.deleteKbCase(..))")
    public void kbCaseDeleteMethods() {}

    @Pointcut("execution(* com.example.iropsim.controller.*.createKbRule(..))")
    public void kbRuleCreateMethods() {}

    @Pointcut("execution(* com.example.iropsim.controller.*.updateKbRule(..))")
    public void kbRuleUpdateMethods() {}

    @Pointcut("execution(* com.example.iropsim.controller.*.deleteKbRule(..))")
    public void kbRuleDeleteMethods() {}

    @AfterReturning(pointcut = "alarmAckMethods()", returning = "result")
    public void logAlarmAck(JoinPoint joinPoint, Object result) {
        HttpServletRequest request = getCurrentRequest();
        if (request != null && joinPoint.getArgs().length > 0) {
            String alarmId = joinPoint.getArgs()[0].toString();
            auditLogService.logAlarmAck(request, alarmId);
        }
    }

    @AfterReturning(pointcut = "kbCaseCreateMethods()", returning = "result")
    public void logKbCaseCreate(JoinPoint joinPoint, Object result) {
        HttpServletRequest request = getCurrentRequest();
        if (request != null && result != null) {
            // 假设返回的对象有getId()方法
            String caseId = extractIdFromResult(result);
            auditLogService.logKbCaseCreate(request, caseId);
        }
    }

    @AfterReturning(pointcut = "kbCaseUpdateMethods()", returning = "result")
    public void logKbCaseUpdate(JoinPoint joinPoint, Object result) {
        HttpServletRequest request = getCurrentRequest();
        if (request != null && joinPoint.getArgs().length > 0) {
            String caseId = joinPoint.getArgs()[0].toString();
            auditLogService.logKbCaseUpdate(request, caseId);
        }
    }

    @AfterReturning(pointcut = "kbCaseDeleteMethods()")
    public void logKbCaseDelete(JoinPoint joinPoint) {
        HttpServletRequest request = getCurrentRequest();
        if (request != null && joinPoint.getArgs().length > 0) {
            String caseId = joinPoint.getArgs()[0].toString();
            auditLogService.logKbCaseDelete(request, caseId);
        }
    }

    @AfterReturning(pointcut = "kbRuleCreateMethods()", returning = "result")
    public void logKbRuleCreate(JoinPoint joinPoint, Object result) {
        HttpServletRequest request = getCurrentRequest();
        if (request != null && result != null) {
            String ruleId = extractIdFromResult(result);
            auditLogService.logKbRuleCreate(request, ruleId);
        }
    }

    @AfterReturning(pointcut = "kbRuleUpdateMethods()", returning = "result")
    public void logKbRuleUpdate(JoinPoint joinPoint, Object result) {
        HttpServletRequest request = getCurrentRequest();
        if (request != null && joinPoint.getArgs().length > 0) {
            String ruleId = joinPoint.getArgs()[0].toString();
            auditLogService.logKbRuleUpdate(request, ruleId);
        }
    }

    @AfterReturning(pointcut = "kbRuleDeleteMethods()")
    public void logKbRuleDelete(JoinPoint joinPoint) {
        HttpServletRequest request = getCurrentRequest();
        if (request != null && joinPoint.getArgs().length > 0) {
            String ruleId = joinPoint.getArgs()[0].toString();
            auditLogService.logKbRuleDelete(request, ruleId);
        }
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    private String extractIdFromResult(Object result) {
        // 简化处理，实际应该根据返回对象的类型来提取ID
        return result.toString();
    }
}
