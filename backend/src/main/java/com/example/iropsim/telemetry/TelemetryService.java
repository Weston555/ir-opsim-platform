package com.example.iropsim.telemetry;

import com.example.iropsim.entity.JointSample;
import com.example.iropsim.entity.PoseSample;
import com.example.iropsim.repository.JointSampleRepository;
import com.example.iropsim.repository.PoseSampleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 遥测服务
 */
@Service
@RequiredArgsConstructor
public class TelemetryService {

    private final JointSampleRepository jointSampleRepository;
    private final PoseSampleRepository poseSampleRepository;

    /**
     * 获取机器人最新的关节数据
     */
    public List<JointSample> getLatestJointSamples(UUID robotId) {
        // 获取每个关节的最新样本
        List<JointSample> latestSamples = jointSampleRepository.findAll().stream()
                .filter(sample -> sample.getRobot().getId().equals(robotId))
                .collect(java.util.stream.Collectors.groupingBy(
                        JointSample::getJointIndex,
                        java.util.stream.Collectors.maxBy(java.util.Comparator.comparing(JointSample::getTs))
                ))
                .values()
                .stream()
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .collect(java.util.stream.Collectors.toList());

        return latestSamples;
    }

    /**
     * 获取机器人最新的位姿数据
     */
    public PoseSample getLatestPoseSample(UUID robotId) {
        return poseSampleRepository.findLatestByRobot(robotId);
    }

    /**
     * 获取关节历史数据序列
     */
    public List<JointSample> getJointSeries(UUID robotId, Integer jointIndex,
                                          String metric, Instant from, Instant to, int limit) {
        // 简化实现，实际应该根据metric过滤数据
        return jointSampleRepository.findByRobotAndJointTimeRange(
                robotId, jointIndex, from, to,
                org.springframework.data.domain.PageRequest.of(0, limit)
        );
    }

    /**
     * 获取位姿历史数据序列
     */
    public List<PoseSample> getPoseSeries(UUID robotId, Instant from, Instant to, int limit) {
        return poseSampleRepository.findByRobotTimeRange(
                robotId, from, to,
                org.springframework.data.domain.PageRequest.of(0, limit)
        );
    }
}
