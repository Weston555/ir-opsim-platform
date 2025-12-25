package com.example.iropsim.controller;

import com.example.iropsim.common.ApiResponse;
import com.example.iropsim.entity.AuditLog;
import com.example.iropsim.repository.AuditLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * 审计日志控制器
 */
@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Tag(name = "审计日志", description = "审计日志查询接口")
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "查询审计日志", description = "分页查询审计日志记录")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getAuditLogs(
            @Parameter(description = "页码(从0开始)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "操作人用户ID") @RequestParam(required = false) String actorUserId,
            @Parameter(description = "操作类型") @RequestParam(required = false) String action,
            @Parameter(description = "开始时间") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @Parameter(description = "结束时间") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("ts").descending());
        Page<AuditLog> auditLogs = auditLogRepository.findFiltered(actorUserId, action, from, to, pageable);

        return ResponseEntity.ok(ApiResponse.success(auditLogs));
    }
}
