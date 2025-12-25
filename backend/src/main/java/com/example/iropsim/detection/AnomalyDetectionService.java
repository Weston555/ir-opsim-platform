package com.example.iropsim.detection;

import com.example.iropsim.alarm.AlarmService;
import com.example.iropsim.entity.*;
import com.example.iropsim.repository.AlarmEventRepository;
import com.example.iropsim.repository.RobotRepository;
import com.example.iropsim.websocket.WebSocketEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * 异常检测服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnomalyDetectionService {

    private final SlidingWindowBuffer slidingWindowBuffer;
    private final ThresholdDetector thresholdDetector;
    private final ZScoreDetector zScoreDetector;
    private final AlarmEventRepository alarmEventRepository;
    private final RobotRepository robotRepository;
    private final WebSocketEventHandler webSocketEventHandler;
    private final AlarmService alarmService;

    /**
     * 处理新的传感器样本并执行异常检测
     */
    @Transactional
    public void processSample(JointSample jointSample) {
        String robotId = jointSample.getRobot().getId().toString();
        int jointIndex = jointSample.getJointIndex();

        // 添加到滑动窗口
        slidingWindowBuffer.addSample(robotId, jointIndex, jointSample.getCurrentA(), jointSample.getTs());
        slidingWindowBuffer.addSample(robotId, jointIndex, jointSample.getVibrationRms(), jointSample.getTs());
        slidingWindowBuffer.addSample(robotId, jointIndex, jointSample.getTemperatureC(), jointSample.getTs());

        // 对每个指标执行检测
        detectAnomaly(jointSample, "CURRENT", jointSample.getCurrentA());
        detectAnomaly(jointSample, "VIBRATION", jointSample.getVibrationRms());
        detectAnomaly(jointSample, "TEMPERATURE", jointSample.getTemperatureC());
    }

    /**
     * 处理位姿样本的异常检测
     */
    @Transactional
    public void processSample(PoseSample poseSample) {
        // 位姿数据的异常检测（简化实现，重点关注位置偏差）
        String robotId = poseSample.getRobot().getId().toString();

        // 计算位置变化率（简化版）
        double positionMagnitude = Math.sqrt(
            poseSample.getX() * poseSample.getX() +
            poseSample.getY() * poseSample.getY() +
            poseSample.getZ() * poseSample.getZ()
        );

        // 这里可以添加位姿异常检测逻辑
        // 暂时跳过位姿检测，专注于关节数据
    }

    /**
     * 执行异常检测
     */
    private void detectAnomaly(JointSample jointSample, String metricType, double value) {
        String robotId = jointSample.getRobot().getId().toString();
        int jointIndex = jointSample.getJointIndex();

        // 获取滑动窗口内的历史数据
        List<Double> historicalValues = slidingWindowBuffer.getValuesInWindow(robotId, jointIndex, jointSample.getTs());

        if (historicalValues.size() < 10) {
            return; // 数据不足，跳过检测
        }

        // 使用阈值检测器
        DetectionResult thresholdResult = thresholdDetector.detect(historicalValues);
        if (thresholdResult.isAnomaly()) {
            createOrUpdateAlarm(jointSample, metricType, "THRESHOLD", thresholdResult);
        }

        // 使用Z-Score检测器
        DetectionResult zScoreResult = zScoreDetector.detect(historicalValues);
        if (zScoreResult.isAnomaly()) {
            createOrUpdateAlarm(jointSample, metricType, "Z_SCORE", zScoreResult);
        }
    }

    /**
     * 创建或更新告警事件
     */
    private void createOrUpdateAlarm(JointSample jointSample, String metricType,
                                   String detectorType, DetectionResult detectionResult) {
        String robotId = jointSample.getRobot().getId().toString();
        int jointIndex = jointSample.getJointIndex();

        // 生成去重键
        String dedupKey = String.format("%s-%d-%s-%s",
            robotId, jointIndex, metricType.toLowerCase(), detectorType.toLowerCase());

        // 查找现有告警
        Optional<AlarmEvent> existingAlarm = alarmEventRepository.findByDedupKey(dedupKey);

        AlarmEvent alarmEvent;
        boolean isNew = false;

        if (existingAlarm.isPresent()) {
            // 更新现有告警
            alarmEvent = existingAlarm.get();
            alarmEvent.setLastSeenTs(jointSample.getTs());
            alarmEvent.setCount(alarmEvent.getCount() + 1);
            alarmEvent.setScore(Math.max(alarmEvent.getScore(), detectionResult.getScore()));
            alarmEvent.setUpdatedAt(Instant.now());
        } else {
            // 创建新告警
            isNew = true;
            String alarmType = getAlarmType(metricType);

            alarmEvent = AlarmEvent.builder()
                    .firstSeenTs(jointSample.getTs())
                    .lastSeenTs(jointSample.getTs())
                    .robot(jointSample.getRobot())
                    .jointIndex(jointIndex)
                    .alarmType(AlarmEvent.AlarmType.valueOf(alarmType))
                    .severity(AlarmEvent.Severity.valueOf(detectionResult.getSeverity()))
                    .status(AlarmEvent.Status.OPEN)
                    .dedupKey(dedupKey)
                    .count(1)
                    .detector(detectorType)
                    .score(detectionResult.getScore())
                    .evidence(detectionResult.getEvidence())
                    .scenarioRun(jointSample.getScenarioRun())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
        }

        // 检查是否应该抑制告警
        boolean shouldSuppress = false;
        if (isNew) {
            shouldSuppress = alarmService.shouldSuppressAlarm(alarmEvent);
        }

        // 如果需要抑制，更新告警状态
        if (shouldSuppress) {
            alarmService.updateAlarmSuppression(alarmEvent, true);
        }

        alarmEvent = alarmEventRepository.save(alarmEvent);

        // 只有非抑制的告警才推送
        if (!shouldSuppress) {
            webSocketEventHandler.pushAlarmEvent(alarmEvent);
        }

        log.info("Alarm event {}: {} - {} - score: {} - suppressed: {}",
            isNew ? "created" : "updated",
            alarmEvent.getAlarmType(),
            alarmEvent.getSeverity(),
            alarmEvent.getScore(),
            shouldSuppress);
    }

    /**
     * 根据指标类型获取告警类型
     */
    private String getAlarmType(String metricType) {
        switch (metricType) {
            case "CURRENT": return "CURRENT_ANOMALY";
            case "VIBRATION": return "VIB_ANOMALY";
            case "TEMPERATURE": return "TEMP_ANOMALY";
            default: return "UNKNOWN_ANOMALY";
        }
    }
}
