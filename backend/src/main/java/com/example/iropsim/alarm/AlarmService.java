package com.example.iropsim.alarm;

import com.example.iropsim.audit.AuditLogService;
import com.example.iropsim.auth.UserDetailsImpl;
import com.example.iropsim.entity.*;
import com.example.iropsim.repository.AlarmAckRepository;
import com.example.iropsim.repository.AlarmEventRepository;
import com.example.iropsim.repository.UserRepository;
import com.example.iropsim.websocket.WebSocketEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.UUID;

/**
 * 告警服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmService {

    private final AlarmEventRepository alarmEventRepository;
    private final AlarmAckRepository alarmAckRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final WebSocketEventHandler webSocketEventHandler;

    /**
     * 确认告警
     */
    @Transactional
    public AlarmAck acknowledgeAlarm(UUID alarmId, String comment, HttpServletRequest request) {
        AlarmEvent alarmEvent = alarmEventRepository.findById(alarmId)
                .orElseThrow(() -> new IllegalArgumentException("Alarm not found: " + alarmId));

        if (alarmEvent.getStatus() == AlarmEvent.Status.CLOSED) {
            throw new IllegalStateException("Cannot acknowledge a closed alarm");
        }

        // 获取当前用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User ackBy = userRepository.findById(UUID.fromString(userDetails.getId()))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 更新告警状态
        alarmEvent.setStatus(AlarmEvent.Status.ACKED);
        alarmEvent.setUpdatedAt(Instant.now());
        alarmEventRepository.save(alarmEvent);

        // 创建确认记录
        AlarmAck alarmAck = AlarmAck.builder()
                .alarm(alarmEvent)
                .ackBy(ackBy)
                .ackTs(Instant.now())
                .comment(comment)
                .createdAt(Instant.now())
                .build();

        alarmAck = alarmAckRepository.save(alarmAck);

        // 推送更新
        webSocketEventHandler.pushAlarmEvent(alarmEvent);

        // 记录审计日志
        auditLogService.logAlarmAck(request, alarmId.toString());

        log.info("Alarm {} acknowledged by user {}", alarmId, ackBy.getUsername());
        return alarmAck;
    }

    /**
     * 关闭告警
     */
    @Transactional
    public void closeAlarm(UUID alarmId) {
        AlarmEvent alarmEvent = alarmEventRepository.findById(alarmId)
                .orElseThrow(() -> new IllegalArgumentException("Alarm not found: " + alarmId));

        alarmEvent.setStatus(AlarmEvent.Status.CLOSED);
        alarmEvent.setUpdatedAt(Instant.now());
        alarmEventRepository.save(alarmEvent);

        // 推送更新
        webSocketEventHandler.pushAlarmEvent(alarmEvent);

        log.info("Alarm {} closed", alarmId);
    }

    /**
     * 检查是否应该抑制告警
     * 抑制策略：同robot同alarm_type在60秒内超过3次更新时抑制
     */
    public boolean shouldSuppressAlarm(AlarmEvent alarmEvent) {
        // 查询最近60秒内的类似告警
        Instant oneMinuteAgo = Instant.now().minusSeconds(60);
        long recentCount = alarmEventRepository.countByRobotAndAlarmTypeAndTimeRange(
                alarmEvent.getRobot().getId(),
                alarmEvent.getAlarmType(),
                oneMinuteAgo,
                Instant.now()
        );

        // 如果最近60秒内有超过3个相同类型的告警，则抑制
        boolean shouldSuppress = recentCount >= 3;

        if (shouldSuppress) {
            log.warn("Suppressing alarm {} due to high frequency ({} alarms in last 60 seconds)",
                    alarmEvent.getId(), recentCount);
        }

        return shouldSuppress;
    }

    /**
     * 更新告警的抑制状态
     */
    @Transactional
    public void updateAlarmSuppression(AlarmEvent alarmEvent, boolean suppressed) {
        // 这里可以扩展alarm_event表，添加suppressed字段
        // 目前先通过evidence字段记录抑制状态
        var evidence = alarmEvent.getEvidence();
        if (evidence instanceof java.util.Map) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> evidenceMap = (java.util.Map<String, Object>) evidence;
            evidenceMap.put("suppressed", suppressed);
            evidenceMap.put("suppressedAt", Instant.now().toString());
            alarmEvent.setEvidence(evidenceMap);
            alarmEventRepository.save(alarmEvent);
        }
    }
}
