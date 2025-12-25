package com.example.iropsim.controller;

import com.example.iropsim.common.ApiResponse;
import com.example.iropsim.entity.JointSample;
import com.example.iropsim.entity.PoseSample;
import com.example.iropsim.telemetry.TelemetryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 遥测数据控制器
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "遥测数据", description = "传感器数据查询接口")
public class TelemetryController {

    private final TelemetryService telemetryService;

    @GetMapping("/robots/{robotId}/latest")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('VIEWER')")
    @Operation(summary = "获取机器人最新传感器数据", description = "获取指定机器人的最新关节和位姿数据")
    public ResponseEntity<ApiResponse<LatestTelemetryData>> getLatestTelemetry(@PathVariable UUID robotId) {
        List<JointSample> jointSamples = telemetryService.getLatestJointSamples(robotId);
        PoseSample poseSample = telemetryService.getLatestPoseSample(robotId);

        LatestTelemetryData data = new LatestTelemetryData(jointSamples, poseSample);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/robots/{robotId}/joints/{jointIndex}/series")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('VIEWER')")
    @Operation(summary = "获取关节数据序列", description = "获取指定机器人关节的历史数据序列")
    public ResponseEntity<ApiResponse<List<JointSample>>> getJointSeries(
            @PathVariable UUID robotId,
            @PathVariable Integer jointIndex,
            @Parameter(description = "指标类型") @RequestParam(defaultValue = "current_a") String metric,
            @Parameter(description = "开始时间") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @Parameter(description = "结束时间") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @Parameter(description = "步长/限制") @RequestParam(defaultValue = "100") int step) {

        // 如果没有指定时间范围，默认查询最近1小时
        Instant now = Instant.now();
        if (from == null) from = now.minusSeconds(3600);
        if (to == null) to = now;

        List<JointSample> series = telemetryService.getJointSeries(robotId, jointIndex, metric, from, to, step);
        return ResponseEntity.ok(ApiResponse.success(series));
    }

    @GetMapping("/robots/{robotId}/pose/series")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('VIEWER')")
    @Operation(summary = "获取位姿数据序列", description = "获取指定机器人位姿的历史数据序列")
    public ResponseEntity<ApiResponse<List<PoseSample>>> getPoseSeries(
            @PathVariable UUID robotId,
            @Parameter(description = "开始时间") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @Parameter(description = "结束时间") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @Parameter(description = "步长/限制") @RequestParam(defaultValue = "100") int step) {

        // 如果没有指定时间范围，默认查询最近1小时
        Instant now = Instant.now();
        if (from == null) from = now.minusSeconds(3600);
        if (to == null) to = now;

        List<PoseSample> series = telemetryService.getPoseSeries(robotId, from, to, step);
        return ResponseEntity.ok(ApiResponse.success(series));
    }

    /**
     * 最新遥测数据DTO
     */
    public static class LatestTelemetryData {
        private final List<JointSample> jointSamples;
        private final PoseSample poseSample;

        public LatestTelemetryData(List<JointSample> jointSamples, PoseSample poseSample) {
            this.jointSamples = jointSamples;
            this.poseSample = poseSample;
        }

        public List<JointSample> getJointSamples() {
            return jointSamples;
        }

        public PoseSample getPoseSample() {
            return poseSample;
        }
    }
}
