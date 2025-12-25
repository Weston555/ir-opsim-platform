package com.example.iropsim.kb;

import com.example.iropsim.entity.*;
import com.example.iropsim.repository.KbCaseRepository;
import com.example.iropsim.repository.KbRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 建议服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final KbRuleRepository kbRuleRepository;
    private final KbCaseRepository kbCaseRepository;
    private final RuleEngine ruleEngine;

    /**
     * 为告警生成建议
     */
    public Recommendation getRecommendationForAlarm(UUID alarmId) {
        // 这里需要通过AlarmEventRepository获取告警，但为了简化，我们假设alarm参数已经提供
        // 实际使用时需要修改方法签名
        throw new UnsupportedOperationException("This method needs to be called with AlarmEvent parameter");
    }

    /**
     * 为告警生成建议
     */
    public Recommendation getRecommendationForAlarm(AlarmEvent alarmEvent) {
        List<MatchedRule> matchedRules = new ArrayList<>();
        List<MatchedCase> matchedCases = new ArrayList<>();

        // 1. 查找匹配的规则
        List<KbRule> enabledRules = kbRuleRepository.findEnabledRulesOrderedByPriority();
        for (KbRule rule : enabledRules) {
            if (ruleEngine.evaluateRule(rule, alarmEvent)) {
                matchedRules.add(new MatchedRule(rule, "Rule matched alarm conditions"));
            }
        }

        // 2. 查找匹配的案例（基于告警类型）
        List<KbCase> relevantCases = kbCaseRepository.findByFaultType(alarmEvent.getAlarmType().toString());
        for (KbCase kbCase : relevantCases) {
            // 简单的匹配逻辑：基于告警类型
            matchedCases.add(new MatchedCase(kbCase, "Case matches alarm type"));
        }

        return new Recommendation(matchedRules, matchedCases,
            generateExplanation(alarmEvent, matchedRules, matchedCases));
    }

    /**
     * 生成解释文本
     */
    private String generateExplanation(AlarmEvent alarmEvent, List<MatchedRule> rules, List<MatchedCase> cases) {
        StringBuilder explanation = new StringBuilder();
        explanation.append("告警类型: ").append(alarmEvent.getAlarmType()).append("\n");
        explanation.append("检测器: ").append(alarmEvent.getDetector()).append("\n");
        explanation.append("严重程度: ").append(alarmEvent.getSeverity()).append("\n");
        explanation.append("异常分数: ").append(String.format("%.2f", alarmEvent.getScore())).append("\n\n");

        if (!rules.isEmpty()) {
            explanation.append("匹配规则数量: ").append(rules.size()).append("\n");
        }

        if (!cases.isEmpty()) {
            explanation.append("相关案例数量: ").append(cases.size()).append("\n");
        }

        return explanation.toString();
    }

    /**
     * 建议结果
     */
    public static class Recommendation {
        private final List<MatchedRule> matchedRules;
        private final List<MatchedCase> matchedCases;
        private final String explanation;

        public Recommendation(List<MatchedRule> matchedRules, List<MatchedCase> matchedCases, String explanation) {
            this.matchedRules = matchedRules;
            this.matchedCases = matchedCases;
            this.explanation = explanation;
        }

        public List<MatchedRule> getMatchedRules() {
            return matchedRules;
        }

        public List<MatchedCase> getMatchedCases() {
            return matchedCases;
        }

        public String getExplanation() {
            return explanation;
        }
    }

    /**
     * 匹配的规则
     */
    public static class MatchedRule {
        private final KbRule rule;
        private final String reason;

        public MatchedRule(KbRule rule, String reason) {
            this.rule = rule;
            this.reason = reason;
        }

        public KbRule getRule() {
            return rule;
        }

        public String getReason() {
            return reason;
        }
    }

    /**
     * 匹配的案例
     */
    public static class MatchedCase {
        private final KbCase kbCase;
        private final String reason;

        public MatchedCase(KbCase kbCase, String reason) {
            this.kbCase = kbCase;
            this.reason = reason;
        }

        public KbCase getKbCase() {
            return kbCase;
        }

        public String getReason() {
            return reason;
        }
    }
}
