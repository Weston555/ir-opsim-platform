package com.example.iropsim.controller;

import com.example.iropsim.common.ApiResponse;
import com.example.iropsim.entity.Robot;
import com.example.iropsim.repository.RobotRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 机器人控制器
 */
@RestController
@RequestMapping("/api/v1/robots")
@RequiredArgsConstructor
@Tag(name = "机器人", description = "机器人管理接口")
public class RobotController {

    private final RobotRepository robotRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('VIEWER')")
    @Operation(summary = "获取机器人列表", description = "获取所有可用机器人的列表")
    public ResponseEntity<ApiResponse<List<Robot>>> getRobots() {
        List<Robot> robots = robotRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success(robots));
    }
}
