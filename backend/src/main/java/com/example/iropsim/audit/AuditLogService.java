package com.example.iropsim.audit;

import com.example.iropsim.auth.UserDetailsImpl;
import com.example.iropsim.entity.AuditLog;
import com.example.iropsim.entity.User;
import com.example.iropsim.repository.AuditLogRepository;
import com.example.iropsim.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * 审计日志服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public void logAction(HttpServletRequest request, String action, String resource, String resourceId, Object detail) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User actor = null;

            if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
                UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
                actor = userRepository.findById(UUID.fromString(userDetails.getId())).orElse(null);
            }

            com.fasterxml.jackson.databind.JsonNode detailJson = null;
            if (detail != null) {
                try {
                    detailJson = objectMapper.valueToTree(detail);
                } catch (Exception e) {
                    log.warn("Failed to serialize audit detail: {}", e.getMessage());
                }
            }

            AuditLog auditLog = AuditLog.builder()
                    .ts(Instant.now())
                    .actorUser(actor)
                    .action(action)
                    .resource(resource)
                    .resourceId(resourceId)
                    .ip(getClientIp(request))
                    .userAgent(request.getHeader("User-Agent"))
                    .detail(detailJson)
                    .build();

            auditLogRepository.save(auditLog);

        } catch (Exception e) {
            log.error("Failed to create audit log for action: {}", action, e);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    // 便捷方法
    public void logLogin(HttpServletRequest request, String username, boolean success) {
        logAction(request, success ? "LOGIN_SUCCESS" : "LOGIN_FAILED", "USER", username,
                success ? "Login successful" : "Login failed");
    }

    public void logAlarmAck(HttpServletRequest request, String alarmId) {
        logAction(request, "ACK_ALARM", "ALARM", alarmId, "Alarm acknowledged");
    }

    public void logKbCaseCreate(HttpServletRequest request, String caseId) {
        logAction(request, "CREATE_KB_CASE", "KB_CASE", caseId, "Knowledge case created");
    }

    public void logKbCaseUpdate(HttpServletRequest request, String caseId) {
        logAction(request, "UPDATE_KB_CASE", "KB_CASE", caseId, "Knowledge case updated");
    }

    public void logKbCaseDelete(HttpServletRequest request, String caseId) {
        logAction(request, "DELETE_KB_CASE", "KB_CASE", caseId, "Knowledge case deleted");
    }

    public void logKbRuleCreate(HttpServletRequest request, String ruleId) {
        logAction(request, "CREATE_KB_RULE", "KB_RULE", ruleId, "Knowledge rule created");
    }

    public void logKbRuleUpdate(HttpServletRequest request, String ruleId) {
        logAction(request, "UPDATE_KB_RULE", "KB_RULE", ruleId, "Knowledge rule updated");
    }

    public void logKbRuleDelete(HttpServletRequest request, String ruleId) {
        logAction(request, "DELETE_KB_RULE", "KB_RULE", ruleId, "Knowledge rule deleted");
    }
}
