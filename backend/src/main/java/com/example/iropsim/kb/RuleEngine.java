package com.example.iropsim.kb;

import com.example.iropsim.entity.AlarmEvent;
import com.example.iropsim.entity.KbRule;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 规则引擎
 * 简化的JSON规则评估器
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuleEngine {

    private final ObjectMapper objectMapper;

    /**
     * 评估规则是否匹配告警事件
     */
    public boolean evaluateRule(KbRule rule, AlarmEvent alarmEvent) {
        try {
            JsonNode whenExpr = rule.getWhenExpr();
            if (whenExpr == null || whenExpr.isNull()) {
                return false;
            }

            // 将告警事件转换为上下文Map
            Map<String, Object> context = createContextFromAlarm(alarmEvent);

            // 评估规则表达式
            return evaluateExpression(whenExpr, context);

        } catch (Exception e) {
            log.error("Error evaluating rule {} for alarm {}: {}", rule.getId(), alarmEvent.getId(), e.getMessage());
            return false;
        }
    }

    /**
     * 从告警事件创建评估上下文
     */
    private Map<String, Object> createContextFromAlarm(AlarmEvent alarmEvent) {
        return Map.of(
            "alarmType", alarmEvent.getAlarmType().toString(),
            "severity", alarmEvent.getSeverity().toString(),
            "score", alarmEvent.getScore(),
            "count", alarmEvent.getCount(),
            "robotId", alarmEvent.getRobot().getId().toString(),
            "jointIndex", alarmEvent.getJointIndex(),
            "detector", alarmEvent.getDetector()
        );
    }

    /**
     * 评估JSON表达式
     * 简化的规则评估器，支持基本的条件判断
     */
    private boolean evaluateExpression(JsonNode expr, Map<String, Object> context) {
        if (expr.isObject()) {
            // 处理AND条件
            if (expr.has("and")) {
                JsonNode conditions = expr.get("and");
                if (conditions.isArray()) {
                    for (JsonNode condition : conditions) {
                        if (!evaluateExpression(condition, context)) {
                            return false;
                        }
                    }
                    return true;
                }
            }

            // 处理OR条件
            if (expr.has("or")) {
                JsonNode conditions = expr.get("or");
                if (conditions.isArray()) {
                    for (JsonNode condition : conditions) {
                        if (evaluateExpression(condition, context)) {
                            return true;
                        }
                    }
                    return false;
                }
            }

            // 处理单个条件
            return evaluateCondition(expr, context);
        }

        return false;
    }

    /**
     * 评估单个条件
     */
    private boolean evaluateCondition(JsonNode condition, Map<String, Object> context) {
        if (!condition.has("field") || !condition.has("op")) {
            return false;
        }

        String field = condition.get("field").asText();
        String op = condition.get("op").asText();

        Object fieldValue = context.get(field);
        if (fieldValue == null && !condition.has("value")) {
            return false;
        }

        JsonNode expectedValue = condition.get("value");

        switch (op) {
            case "equals":
                return equals(fieldValue, expectedValue);
            case "not_equals":
                return !equals(fieldValue, expectedValue);
            case "greater_than":
                return compare(fieldValue, expectedValue) > 0;
            case "less_than":
                return compare(fieldValue, expectedValue) < 0;
            case "contains":
                return contains(fieldValue, expectedValue);
            default:
                log.warn("Unsupported operation: {}", op);
                return false;
        }
    }

    private boolean equals(Object actual, JsonNode expected) {
        if (actual == null) return expected.isNull();
        return actual.toString().equals(expected.asText());
    }

    private int compare(Object actual, JsonNode expected) {
        if (actual instanceof Number && expected.isNumber()) {
            double actualNum = ((Number) actual).doubleValue();
            double expectedNum = expected.asDouble();
            return Double.compare(actualNum, expectedNum);
        }
        return 0;
    }

    private boolean contains(Object actual, JsonNode expected) {
        if (actual instanceof String) {
            return ((String) actual).contains(expected.asText());
        }
        return false;
    }
}
