package com.example.iropsim.controller;

import com.example.iropsim.common.ApiResponse;
import com.example.iropsim.entity.FaultInjection;
import com.example.iropsim.entity.Scenario;
import com.example.iropsim.entity.ScenarioRun;
import com.example.iropsim.repository.FaultInjectionRepository;
import com.example.iropsim.repository.ScenarioRepository;
import com.example.iropsim.repository.ScenarioRunRepository;
import com.example.iropsim.sim.FaultInjectionRequest;
import com.example.iropsim.sim.ScenarioRunRequest;
import com.example.iropsim.sim.SimulationEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

/**
 * 仿真控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/sim")
@RequiredArgsConstructor
@Tag(name = "仿真", description = "仿真运行管理接口")
public class SimulationController {

    private final ScenarioRepository scenarioRepository;
    private final ScenarioRunRepository scenarioRunRepository;
    private final FaultInjectionRepository faultInjectionRepository;
    private final SimulationEngine simulationEngine;
    private final ObjectMapper objectMapper;

    @PostMapping("/runs")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "创建仿真运行", description = "创建新的仿真运行实例")
    public ResponseEntity<ApiResponse<ScenarioRun>> createScenarioRun(@Valid @RequestBody ScenarioRunRequest request) {
        // 获取场景
        Scenario scenario = scenarioRepository.findById(request.getScenarioId())
                .orElseThrow(() -> new IllegalArgumentException("Scenario not found: " + request.getScenarioId()));

        // 创建ScenarioRun实体
        ScenarioRun scenarioRun = ScenarioRun.builder()
                .scenario(scenario)
                .mode(ScenarioRun.RunMode.valueOf(request.getMode().toUpperCase()))
                .seed(request.getSeed() != null ? request.getSeed() : System.currentTimeMillis())
                .rateHz(request.getRateHz())
                .status(ScenarioRun.RunStatus.CREATED)
                .createdAt(Instant.now())
                .build();

        scenarioRun = scenarioRunRepository.save(scenarioRun);
        log.info("Created scenario run: {} for scenario: {}", scenarioRun.getId(), scenario.getName());

        return ResponseEntity.ok(ApiResponse.success("仿真运行创建成功", scenarioRun));
    }

    @PostMapping("/runs/{id}/start")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "启动仿真运行", description = "启动指定的仿真运行")
    public ResponseEntity<ApiResponse<String>> startScenarioRun(@PathVariable UUID id) {
        simulationEngine.startSimulation(id);
        return ResponseEntity.ok(ApiResponse.success("仿真运行启动成功"));
    }

    @PostMapping("/runs/{id}/stop")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "停止仿真运行", description = "停止指定的仿真运行")
    public ResponseEntity<ApiResponse<String>> stopScenarioRun(@PathVariable UUID id) {
        simulationEngine.stopSimulation(id);
        return ResponseEntity.ok(ApiResponse.success("仿真运行停止成功"));
    }

    @GetMapping("/runs/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('VIEWER')")
    @Operation(summary = "获取仿真运行状态", description = "获取指定仿真运行的详细信息")
    public ResponseEntity<ApiResponse<ScenarioRun>> getScenarioRun(@PathVariable UUID id) {
        ScenarioRun scenarioRun = simulationEngine.getSimulationStatus(id);
        return ResponseEntity.ok(ApiResponse.success(scenarioRun));
    }

    @PostMapping("/runs/{id}/faults")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "添加故障注入", description = "为指定的仿真运行添加故障注入")
    public ResponseEntity<ApiResponse<FaultInjection>> addFaultInjection(
            @PathVariable UUID id,
            @Valid @RequestBody FaultInjectionRequest request) {

        ScenarioRun scenarioRun = scenarioRunRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Scenario run not found: " + id));

        FaultInjection faultInjection = FaultInjection.builder()
                .scenarioRun(scenarioRun)
                .faultType(request.getFaultType())
                .startTs(request.getStartTs())
                .endTs(request.getEndTs())
                .params(request.getParams())
                .createdAt(Instant.now())
                .build();

        faultInjection = faultInjectionRepository.save(faultInjection);
        log.info("Added fault injection to scenario run {}: {}", id, faultInjection.getFaultType());

        return ResponseEntity.ok(ApiResponse.success("故障注入添加成功", faultInjection));
    }
}
