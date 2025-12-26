package com.example.iropsim.sim;

import com.example.iropsim.entity.AlarmEvent;
import com.example.iropsim.entity.JointSample;
import com.example.iropsim.entity.PoseSample;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 评测报告
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationReport {

    // 基础信息
    private String scenarioRunId;
    private String scenarioName;
    private String robotName;
    private Instant startTime;
    private Instant endTime;
    private Duration duration;
    private double samplingRateHz;

    // 样本统计
    private SampleStatistics sampleStats;

    // 告警统计
    private AlarmStatistics alarmStats;

    // 检测器性能
    private DetectorPerformance detectorPerformance;

    // 故障注入信息
    private List<FaultInjectionSummary> faultInjections;

    // 详细数据（可选，用于导出）
    private List<JointSample> jointSamples;
    private List<PoseSample> poseSamples;
    private List<AlarmEvent> alarmEvents;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SampleStatistics {
        private int totalJointSamples;
        private int totalPoseSamples;
        private int jointsCount;
        private Duration timeSpan;
        private double avgSamplingIntervalMs;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlarmStatistics {
        private int totalAlarms;
        private int openAlarms;
        private int acknowledgedAlarms;
        private int suppressedAlarms;
        private Map<String, Integer> alarmsByType; // 按告警类型统计
        private Map<String, Integer> alarmsBySeverity; // 按严重程度统计
        private Map<String, Integer> alarmsByDetector; // 按检测器统计
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetectorPerformance {
        private double thresholdAccuracy;
        private double thresholdPrecision;
        private double thresholdRecall;
        private double zScoreAccuracy;
        private double zScorePrecision;
        private double zScoreRecall;
        private Duration avgDetectionDelay;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FaultInjectionSummary {
        private String faultType;
        private Instant startTime;
        private Instant endTime;
        private Duration duration;
        private String params;
        private boolean triggeredAlarm;
        private Duration detectionDelay;
    }
}
