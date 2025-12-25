package com.example.iropsim.controller;

import com.example.iropsim.alarm.AlarmService;
import com.example.iropsim.common.ApiResponse;
import com.example.iropsim.entity.AlarmAck;
import com.example.iropsim.entity.AlarmEvent;
import com.example.iropsim.kb.RecommendationService;
import com.example.iropsim.repository.AlarmEventRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.UUID;

/**
 * 告警控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/alarms")
@RequiredArgsConstructor
@Tag(name = "告警管理", description = "告警查询和管理接口")
public class AlarmController {

    private final AlarmEventRepository alarmEventRepository;
    private final AlarmService alarmService;
    private final RecommendationService recommendationService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('VIEWER')")
    @Operation(summary = "查询告警列表", description = "分页查询告警事件，支持多种筛选条件")
    public ResponseEntity<ApiResponse<Page<AlarmEvent>>> getAlarms(
            @Parameter(description = "页码(从0开始)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "告警状态") @RequestParam(required = false) AlarmEvent.Status status,
            @Parameter(description = "告警级别") @RequestParam(required = false) AlarmEvent.Severity severity,
            @Parameter(description = "机器人ID") @RequestParam(required = false) UUID robotId,
            @Parameter(description = "开始时间") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @Parameter(description = "结束时间") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {

        Pageable pageable = PageRequest.of(page, size);
        Page<AlarmEvent> alarms = alarmEventRepository.findFiltered(status, severity, robotId, from, to, pageable);

        return ResponseEntity.ok(ApiResponse.success(alarms));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('VIEWER')")
    @Operation(summary = "获取告警详情", description = "获取指定告警事件的详细信息")
    public ResponseEntity<ApiResponse<AlarmEvent>> getAlarm(@PathVariable UUID id) {
        AlarmEvent alarm = alarmEventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alarm not found: " + id));

        return ResponseEntity.ok(ApiResponse.success(alarm));
    }

    @PostMapping("/{id}/ack")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "确认告警", description = "确认告警并添加备注")
    public ResponseEntity<ApiResponse<AlarmAck>> acknowledgeAlarm(
            @PathVariable UUID id,
            @RequestBody AckRequest request,
            HttpServletRequest httpRequest) {

        AlarmAck alarmAck = alarmService.acknowledgeAlarm(id, request.getComment(), httpRequest);
        return ResponseEntity.ok(ApiResponse.success("告警已确认", alarmAck));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "关闭告警", description = "关闭指定的告警事件")
    public ResponseEntity<ApiResponse<String>> closeAlarm(@PathVariable UUID id) {
        alarmService.closeAlarm(id);
        return ResponseEntity.ok(ApiResponse.success("告警已关闭"));
    }

    @GetMapping("/{id}/recommendation")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('VIEWER')")
    @Operation(summary = "获取告警建议", description = "获取指定告警的处置建议")
    public ResponseEntity<ApiResponse<RecommendationService.Recommendation>> getAlarmRecommendation(@PathVariable UUID id) {
        AlarmEvent alarm = alarmEventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alarm not found: " + id));

        RecommendationService.Recommendation recommendation = recommendationService.getRecommendationForAlarm(alarm);
        return ResponseEntity.ok(ApiResponse.success(recommendation));
    }

    /**
     * 告警确认请求DTO
     */
    public static class AckRequest {
        private String comment;

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }
    }
}
