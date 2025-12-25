package com.example.iropsim.detection;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 阈值检测器
 * 支持静态阈值和动态阈值检测
 */
@Component
public class ThresholdDetector implements Detector<Double> {

    @Override
    public DetectionResult detect(List<Double> values) {
        if (values.isEmpty()) {
            return new DetectionResult(false, 0.0, "INFO", Map.of("reason", "No data available"));
        }

        double latestValue = values.get(values.size() - 1);

        // 默认阈值配置（可以从配置或数据库中读取）
        ThresholdConfig config = new ThresholdConfig(3.0, 80.0, 0.8, 3.0); // 示例配置

        // 检查是否超过阈值
        boolean isHigh = latestValue > config.upperThreshold;
        boolean isLow = latestValue < config.lowerThreshold;

        if (isHigh || isLow) {
            double deviation = isHigh ?
                (latestValue - config.upperThreshold) / config.upperThreshold :
                (config.lowerThreshold - latestValue) / config.lowerThreshold;

            String severity = calculateSeverity(deviation);

            ThresholdEvidence evidence = new ThresholdEvidence(
                latestValue, config.upperThreshold, config.lowerThreshold,
                isHigh, isLow, deviation, values.size()
            );

            return new DetectionResult(true, deviation, severity, evidence);
        }

        return new DetectionResult(false, 0.0, "INFO",
            Map.of("latestValue", latestValue, "withinThreshold", true));
    }

    @Override
    public String getType() {
        return "THRESHOLD";
    }

    private String calculateSeverity(double deviation) {
        if (deviation >= 1.0) {
            return "CRITICAL";
        } else if (deviation >= 0.5) {
            return "WARN";
        } else {
            return "INFO";
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ThresholdConfig {
        private double lowerThreshold;  // 下阈值
        private double upperThreshold;  // 上阈值
        private double warnMultiplier;  // 警告倍数
        private double criticalMultiplier; // 严重倍数
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ThresholdEvidence {
        private double latestValue;
        private double upperThreshold;
        private double lowerThreshold;
        private boolean exceededUpper;
        private boolean belowLower;
        private double deviation;
        private int windowSize;
    }
}
