package com.example.iropsim.controller;

import com.example.iropsim.audit.AuditLogService;
import com.example.iropsim.auth.UserDetailsImpl;
import com.example.iropsim.common.ApiResponse;
import com.example.iropsim.entity.KbCase;
import com.example.iropsim.entity.KbRule;
import com.example.iropsim.entity.User;
import com.example.iropsim.kb.RecommendationService;
import com.example.iropsim.repository.KbCaseRepository;
import com.example.iropsim.repository.KbRuleRepository;
import com.example.iropsim.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 知识库控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/kb")
@RequiredArgsConstructor
@Tag(name = "知识库", description = "知识库管理和建议生成接口")
public class KnowledgeController {

    private final KbCaseRepository kbCaseRepository;
    private final KbRuleRepository kbRuleRepository;
    private final UserRepository userRepository;
    private final RecommendationService recommendationService;
    private final AuditLogService auditLogService;

    // ===== 案例管理 =====

    @GetMapping("/cases")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('VIEWER')")
    @Operation(summary = "查询知识案例", description = "分页查询知识案例，支持关键词搜索")
    public ResponseEntity<ApiResponse<Page<KbCase>>> getCases(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String faultType,
            @RequestParam(required = false) String tag) {

        Pageable pageable = PageRequest.of(page, size);
        Page<KbCase> cases = kbCaseRepository.search(keyword, faultType, tag, pageable);

        return ResponseEntity.ok(ApiResponse.success(cases));
    }

    @PostMapping("/cases")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "创建知识案例", description = "创建新的知识案例")
    public ResponseEntity<ApiResponse<KbCase>> createCase(@RequestBody KbCase kbCase, HttpServletRequest request) {
        User currentUser = getCurrentUser();

        kbCase.setCreatedBy(currentUser);
        kbCase.setCreatedAt(Instant.now());
        kbCase.setUpdatedAt(Instant.now());

        KbCase savedCase = kbCaseRepository.save(kbCase);

        auditLogService.logKbCaseCreate(request, savedCase.getId().toString());

        log.info("Created knowledge case: {}", savedCase.getId());
        return ResponseEntity.ok(ApiResponse.success("案例创建成功", savedCase));
    }

    @PutMapping("/cases/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "更新知识案例", description = "更新指定的知识案例")
    public ResponseEntity<ApiResponse<KbCase>> updateCase(@PathVariable UUID id, @RequestBody KbCase updatedCase,
                                                         HttpServletRequest request) {
        KbCase existingCase = kbCaseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Case not found: " + id));

        existingCase.setTitle(updatedCase.getTitle());
        existingCase.setFaultType(updatedCase.getFaultType());
        existingCase.setSymptoms(updatedCase.getSymptoms());
        existingCase.setRootCause(updatedCase.getRootCause());
        existingCase.setActions(updatedCase.getActions());
        existingCase.setTags(updatedCase.getTags());
        existingCase.setVersion(updatedCase.getVersion());
        existingCase.setUpdatedAt(Instant.now());

        KbCase savedCase = kbCaseRepository.save(existingCase);

        auditLogService.logKbCaseUpdate(request, savedCase.getId().toString());

        return ResponseEntity.ok(ApiResponse.success("案例更新成功", savedCase));
    }

    @DeleteMapping("/cases/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "删除知识案例", description = "删除指定的知识案例")
    public ResponseEntity<ApiResponse<String>> deleteCase(@PathVariable UUID id, HttpServletRequest request) {
        kbCaseRepository.deleteById(id);

        auditLogService.logKbCaseDelete(request, id.toString());

        return ResponseEntity.ok(ApiResponse.success("案例删除成功"));
    }

    // ===== 规则管理 =====

    @GetMapping("/rules")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "查询知识规则", description = "查询所有知识规则")
    public ResponseEntity<ApiResponse<List<KbRule>>> getRules() {
        List<KbRule> rules = kbRuleRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success(rules));
    }

    @PostMapping("/rules")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "创建知识规则", description = "创建新的知识规则")
    public ResponseEntity<ApiResponse<KbRule>> createRule(@RequestBody KbRule kbRule, HttpServletRequest request) {
        kbRule.setCreatedAt(Instant.now());
        kbRule.setUpdatedAt(Instant.now());

        KbRule savedRule = kbRuleRepository.save(kbRule);

        auditLogService.logKbRuleCreate(request, savedRule.getId().toString());

        log.info("Created knowledge rule: {}", savedRule.getId());
        return ResponseEntity.ok(ApiResponse.success("规则创建成功", savedRule));
    }

    @PutMapping("/rules/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "更新知识规则", description = "更新指定的知识规则")
    public ResponseEntity<ApiResponse<KbRule>> updateRule(@PathVariable UUID id, @RequestBody KbRule updatedRule,
                                                         HttpServletRequest request) {
        KbRule existingRule = kbRuleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rule not found: " + id));

        existingRule.setName(updatedRule.getName());
        existingRule.setEnabled(updatedRule.getEnabled());
        existingRule.setPriority(updatedRule.getPriority());
        existingRule.setWhenExpr(updatedRule.getWhenExpr());
        existingRule.setThenCase(updatedRule.getThenCase());
        existingRule.setUpdatedAt(Instant.now());

        KbRule savedRule = kbRuleRepository.save(existingRule);

        auditLogService.logKbRuleUpdate(request, savedRule.getId().toString());

        return ResponseEntity.ok(ApiResponse.success("规则更新成功", savedRule));
    }

    @DeleteMapping("/rules/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "删除知识规则", description = "删除指定的知识规则")
    public ResponseEntity<ApiResponse<String>> deleteRule(@PathVariable UUID id, HttpServletRequest request) {
        kbRuleRepository.deleteById(id);

        auditLogService.logKbRuleDelete(request, id.toString());

        return ResponseEntity.ok(ApiResponse.success("规则删除成功"));
    }

    @PostMapping("/rules/{id}/test")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "测试知识规则", description = "测试规则是否匹配给定的告警JSON")
    public ResponseEntity<ApiResponse<Boolean>> testRule(@PathVariable UUID id, @RequestBody String alarmJson) {
        // 这里可以实现规则测试逻辑
        // 暂时返回true作为占位符
        return ResponseEntity.ok(ApiResponse.success(true));
    }

    /**
     * 获取当前登录用户
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return userRepository.findById(UUID.fromString(userDetails.getId()))
                .orElseThrow(() -> new IllegalStateException("Current user not found"));
    }
}
