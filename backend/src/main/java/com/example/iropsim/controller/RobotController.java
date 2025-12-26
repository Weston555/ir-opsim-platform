package com.example.iropsim.controller;

import com.example.iropsim.common.ApiResponse;
import com.example.iropsim.entity.JointSample;
import com.example.iropsim.entity.PoseSample;
import com.example.iropsim.entity.Robot;
import com.example.iropsim.repository.JointSampleRepository;
import com.example.iropsim.repository.PoseSampleRepository;
import com.example.iropsim.repository.RobotRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 机器人管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/robots")
@RequiredArgsConstructor
@Tag(name = "机器人管理", description = "机器人增删改查管理接口")
public class RobotController {

    private final RobotRepository robotRepository;
    private final JointSampleRepository jointSampleRepository;
    private final PoseSampleRepository poseSampleRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('VIEWER')")
    @Operation(summary = "获取机器人列表", description = "分页获取机器人列表")
    public ResponseEntity<ApiResponse<Page<Robot>>> getRobots(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String model) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Robot> robots;

        if (model != null && !model.trim().isEmpty()) {
            robots = robotRepository.findByModelContainingIgnoreCase(model, pageable);
        } else {
            robots = robotRepository.findAll(pageable);
        }

        return ResponseEntity.ok(ApiResponse.success(robots));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('VIEWER')")
    @Operation(summary = "获取机器人详情", description = "获取指定机器人的详细信息")
    public ResponseEntity<ApiResponse<Robot>> getRobot(@PathVariable UUID id) {
        Robot robot = robotRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Robot not found: " + id));
        return ResponseEntity.ok(ApiResponse.success(robot));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "创建机器人", description = "创建新的机器人")
    public ResponseEntity<ApiResponse<Robot>> createRobot(@Valid @RequestBody CreateRobotRequest request) {
        Robot robot = Robot.builder()
                .name(request.getName())
                .model(request.getModel())
                .jointCount(request.getJointCount())
                .description(request.getDescription())
                .status(Robot.RobotStatus.OFFLINE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        robot = robotRepository.save(robot);
        log.info("Created robot: {}", robot.getName());

        return ResponseEntity.ok(ApiResponse.success("机器人创建成功", robot));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "更新机器人", description = "更新指定机器人的信息")
    public ResponseEntity<ApiResponse<Robot>> updateRobot(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRobotRequest request) {

        Robot robot = robotRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Robot not found: " + id));

        robot.setName(request.getName());
        robot.setModel(request.getModel());
        robot.setJointCount(request.getJointCount());
        robot.setDescription(request.getDescription());
        robot.setUpdatedAt(Instant.now());

        robot = robotRepository.save(robot);
        log.info("Updated robot: {}", robot.getName());

        return ResponseEntity.ok(ApiResponse.success("机器人更新成功", robot));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "删除机器人", description = "删除指定的机器人")
    public ResponseEntity<ApiResponse<String>> deleteRobot(@PathVariable UUID id) {
        Robot robot = robotRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Robot not found: " + id));

        robotRepository.delete(robot);
        log.info("Deleted robot: {}", robot.getName());

        return ResponseEntity.ok(ApiResponse.success("机器人删除成功"));
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "更新机器人状态", description = "更新指定机器人的运行状态")
    public ResponseEntity<ApiResponse<Robot>> updateRobotStatus(
            @PathVariable UUID id,
            @RequestBody UpdateStatusRequest request) {

        Robot robot = robotRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Robot not found: " + id));

        robot.setStatus(Robot.RobotStatus.valueOf(request.getStatus()));
        robot.setUpdatedAt(Instant.now());

        robot = robotRepository.save(robot);
        log.info("Updated robot {} status to: {}", robot.getName(), robot.getStatus());

        return ResponseEntity.ok(ApiResponse.success("机器人状态更新成功", robot));
    }

    // 请求DTO
    public static class CreateRobotRequest {
        private String name;
        private String model;
        private Integer jointCount;
        private String description;

        // getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }

        public Integer getJointCount() { return jointCount; }
        public void setJointCount(Integer jointCount) { this.jointCount = jointCount; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    public static class UpdateRobotRequest {
        private String name;
        private String model;
        private Integer jointCount;
        private String description;

        // getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }

        public Integer getJointCount() { return jointCount; }
        public void setJointCount(Integer jointCount) { this.jointCount = jointCount; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    public static class UpdateStatusRequest {
        private String status;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    @GetMapping("/{id}/telemetry/latest")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('VIEWER')")
    @Operation(summary = "获取机器人最新遥测数据", description = "获取指定机器人的最新传感器数据")
    public ResponseEntity<ApiResponse<TelemetryData>> getLatestTelemetry(@PathVariable UUID id) {
        Robot robot = robotRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Robot not found: " + id));

        // 获取最新的关节数据（每个关节最近一条）
        List<JointSample> latestJointSamples = new ArrayList<>();
        for (int jointIndex = 0; jointIndex < robot.getJointCount(); jointIndex++) {
            List<JointSample> jointSamples = jointSampleRepository
                    .findTopByRobotIdAndJointIndexOrderByTsDesc(id, jointIndex, 1);
            if (!jointSamples.isEmpty()) {
                latestJointSamples.add(jointSamples.get(0));
            }
        }

        // 获取最新的位姿数据
        List<PoseSample> poseSamples = poseSampleRepository
                .findTopByRobotIdOrderByTsDesc(id, 1);
        PoseSample latestPoseSample = poseSamples.isEmpty() ? null : poseSamples.get(0);

        TelemetryData telemetryData = new TelemetryData();
        telemetryData.setRobotId(id.toString());
        telemetryData.setJointSamples(latestJointSamples.stream()
                .map(this::convertJointSample)
                .collect(Collectors.toList()));

        if (latestPoseSample != null) {
            telemetryData.setPoseSample(convertPoseSample(latestPoseSample));
        }

        return ResponseEntity.ok(ApiResponse.success(telemetryData));
    }

    @GetMapping("/{id}/telemetry/series")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('VIEWER')")
    @Operation(summary = "获取机器人时序数据", description = "获取指定机器人的历史传感器数据")
    public ResponseEntity<ApiResponse<List<TelemetrySeriesData>>> getTelemetrySeries(
            @PathVariable UUID id,
            @RequestParam String metric,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "100") int step) {

        Robot robot = robotRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Robot not found: " + id));

        List<TelemetrySeriesData> seriesData = new ArrayList<>();

        if ("current_a".equals(metric) || "vibration_rms".equals(metric) || "temperature_c".equals(metric)) {
            // 查询关节数据
            List<JointSample> samples = jointSampleRepository
                    .findByRobotIdAndTsBetweenOrderByTsDesc(id, from != null ? from : Instant.now().minus(1, ChronoUnit.HOURS), to != null ? to : Instant.now());

            // 按时间间隔采样
            Map<Instant, List<JointSample>> groupedByTime = samples.stream()
                    .collect(Collectors.groupingBy(sample -> {
                        long timestamp = sample.getTs().toEpochMilli();
                        long interval = (to != null && from != null) ?
                                (to.toEpochMilli() - from.toEpochMilli()) / step :
                                3600000L / step; // 默认1小时
                        long bucket = timestamp / interval * interval;
                        return Instant.ofEpochMilli(bucket);
                    }));

            for (Map.Entry<Instant, List<JointSample>> entry : groupedByTime.entrySet()) {
                List<JointSample> timeSamples = entry.getValue();
                double avgValue = timeSamples.stream()
                        .mapToDouble(sample -> getMetricValue(sample, metric))
                        .average().orElse(0.0);

                TelemetrySeriesData data = new TelemetrySeriesData();
                data.setTs(entry.getKey());
                data.setValue(avgValue);
                seriesData.add(data);
            }
        }

        // 按时间排序
        seriesData.sort(Comparator.comparing(TelemetrySeriesData::getTs));

        return ResponseEntity.ok(ApiResponse.success(seriesData));
    }

    private double getMetricValue(JointSample sample, String metric) {
        switch (metric) {
            case "current_a": return sample.getCurrentA();
            case "vibration_rms": return sample.getVibrationRms();
            case "temperature_c": return sample.getTemperatureC();
            default: return 0.0;
        }
    }

    private TelemetryJointSample convertJointSample(JointSample sample) {
        TelemetryJointSample jointSample = new TelemetryJointSample();
        jointSample.setJointIndex(sample.getJointIndex());
        jointSample.setCurrentA(sample.getCurrentA());
        jointSample.setVibrationRms(sample.getVibrationRms());
        jointSample.setTemperatureC(sample.getTemperatureC());
        jointSample.setTs(sample.getTs());
        return jointSample;
    }

    private TelemetryPoseSample convertPoseSample(PoseSample sample) {
        TelemetryPoseSample poseSample = new TelemetryPoseSample();
        poseSample.setX(sample.getX());
        poseSample.setY(sample.getY());
        poseSample.setZ(sample.getZ());
        poseSample.setRx(sample.getRx());
        poseSample.setRy(sample.getRy());
        poseSample.setRz(sample.getRz());
        poseSample.setTs(sample.getTs());
        return poseSample;
    }

    // 内部DTO类
    public static class TelemetryData {
        private String robotId;
        private List<TelemetryJointSample> jointSamples;
        private TelemetryPoseSample poseSample;

        // getters and setters
        public String getRobotId() { return robotId; }
        public void setRobotId(String robotId) { this.robotId = robotId; }

        public List<TelemetryJointSample> getJointSamples() { return jointSamples; }
        public void setJointSamples(List<TelemetryJointSample> jointSamples) { this.jointSamples = jointSamples; }

        public TelemetryPoseSample getPoseSample() { return poseSample; }
        public void setPoseSample(TelemetryPoseSample poseSample) { this.poseSample = poseSample; }
    }

    public static class TelemetryJointSample {
        private int jointIndex;
        private double currentA;
        private double vibrationRms;
        private double temperatureC;
        private Instant ts;

        // getters and setters
        public int getJointIndex() { return jointIndex; }
        public void setJointIndex(int jointIndex) { this.jointIndex = jointIndex; }

        public double getCurrentA() { return currentA; }
        public void setCurrentA(double currentA) { this.currentA = currentA; }

        public double getVibrationRms() { return vibrationRms; }
        public void setVibrationRms(double vibrationRms) { this.vibrationRms = vibrationRms; }

        public double getTemperatureC() { return temperatureC; }
        public void setTemperatureC(double temperatureC) { this.temperatureC = temperatureC; }

        public Instant getTs() { return ts; }
        public void setTs(Instant ts) { this.ts = ts; }
    }

    public static class TelemetryPoseSample {
        private double x, y, z, rx, ry, rz;
        private Instant ts;

        // getters and setters
        public double getX() { return x; }
        public void setX(double x) { this.x = x; }

        public double getY() { return y; }
        public void setY(double y) { this.y = y; }

        public double getZ() { return z; }
        public void setZ(double z) { this.z = z; }

        public double getRx() { return rx; }
        public void setRx(double rx) { this.rx = rx; }

        public double getRy() { return ry; }
        public void setRy(double ry) { this.ry = ry; }

        public double getRz() { return rz; }
        public void setRz(double rz) { this.rz = rz; }

        public Instant getTs() { return ts; }
        public void setTs(Instant ts) { this.ts = ts; }
    }

    public static class TelemetrySeriesData {
        private Instant ts;
        private double value;

        public Instant getTs() { return ts; }
        public void setTs(Instant ts) { this.ts = ts; }

        public double getValue() { return value; }
        public void setValue(double value) { this.value = value; }
    }
}