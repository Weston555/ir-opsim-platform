package com.example.iropsim.detection;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Z-Score检测器
 * 使用统计方法检测异常值：z = (x - μ) / σ
 */
@Component
public class ZScoreDetector implements Detector<Double> {

    // Z-Score阈值
    private static final double Z_THRESHOLD = 3.0;

    @Override
    public DetectionResult detect(List<Double> values) {
        if (values.size() < 10) { // 需要足够的数据进行统计
            return new DetectionResult(false, 0.0, "INFO",
                ZScoreEvidence.builder()
                    .reason("Insufficient data for statistical analysis")
                    .sampleCount(values.size())
                    .build());
        }

        // 计算均值和标准差
        double sum = values.stream().mapToDouble(Double::doubleValue).sum();
        double mean = sum / values.size();

        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .sum() / values.size();
        double stdDev = Math.sqrt(variance);

        if (stdDev == 0) {
            return new DetectionResult(false, 0.0, "INFO",
                ZScoreEvidence.builder()
                    .reason("No variance in data")
                    .mean(mean)
                    .stdDev(stdDev)
                    .sampleCount(values.size())
                    .build());
        }

        // 计算最新值的Z-Score
        double latestValue = values.get(values.size() - 1);
        double zScore = Math.abs((latestValue - mean) / stdDev);

        boolean isAnomaly = zScore > Z_THRESHOLD;

        ZScoreEvidence evidence = ZScoreEvidence.builder()
                .latestValue(latestValue)
                .mean(mean)
                .stdDev(stdDev)
                .zScore(zScore)
                .threshold(Z_THRESHOLD)
                .sampleCount(values.size())
                .isAnomaly(isAnomaly)
                .build();

        String severity = calculateSeverity(zScore);

        return new DetectionResult(isAnomaly, zScore, severity, evidence);
    }

    @Override
    public String getType() {
        return "Z_SCORE";
    }

    private String calculateSeverity(double zScore) {
        if (zScore >= 5.0) {
            return "CRITICAL";
        } else if (zScore >= 3.0) {
            return "WARN";
        } else {
            return "INFO";
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @lombok.Builder
    public static class ZScoreEvidence {
        private String reason;
        private double latestValue;
        private double mean;
        private double stdDev;
        private double zScore;
        private double threshold;
        private int sampleCount;
        private boolean isAnomaly;
    }
}
