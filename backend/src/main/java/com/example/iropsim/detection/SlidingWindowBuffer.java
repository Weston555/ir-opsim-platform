package com.example.iropsim.detection;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 滑动窗口缓冲区
 * 按(robotId, jointIndex)维度维护最近一段时间的样本数据
 */
@Slf4j
@Component
public class SlidingWindowBuffer {

    // 窗口大小（秒）
    private static final int WINDOW_SIZE_SECONDS = 60;

    // 缓冲区：robotId -> jointIndex -> 时间排序的样本列表
    private final Map<String, Map<Integer, Deque<TimestampedValue>>> buffers = new ConcurrentHashMap<>();

    /**
     * 添加样本到缓冲区
     */
    public void addSample(String robotId, int jointIndex, double value, Instant timestamp) {
        String key = robotId;
        buffers.computeIfAbsent(key, k -> new ConcurrentHashMap<>())
               .computeIfAbsent(jointIndex, k -> new LinkedList<>())
               .addLast(new TimestampedValue(value, timestamp));

        // 清理过期数据
        cleanupExpiredData(key, jointIndex);
    }

    /**
     * 获取指定时间窗口内的样本值
     */
    public List<Double> getValuesInWindow(String robotId, int jointIndex, Instant currentTime) {
        String key = robotId;
        Map<Integer, Deque<TimestampedValue>> robotBuffers = buffers.get(key);
        if (robotBuffers == null) {
            return Collections.emptyList();
        }

        Deque<TimestampedValue> jointBuffer = robotBuffers.get(jointIndex);
        if (jointBuffer == null) {
            return Collections.emptyList();
        }

        Instant windowStart = currentTime.minusSeconds(WINDOW_SIZE_SECONDS);

        List<Double> values = new ArrayList<>();
        for (TimestampedValue tv : jointBuffer) {
            if (tv.timestamp.isAfter(windowStart)) {
                values.add(tv.value);
            }
        }

        return values;
    }

    /**
     * 获取缓冲区统计信息
     */
    public BufferStats getBufferStats(String robotId, int jointIndex) {
        List<Double> values = getValuesInWindow(robotId, jointIndex, Instant.now());
        if (values.isEmpty()) {
            return new BufferStats(0, 0.0, 0.0, 0.0, 0.0);
        }

        double sum = values.stream().mapToDouble(Double::doubleValue).sum();
        double mean = sum / values.size();
        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .sum() / values.size();
        double stdDev = Math.sqrt(variance);

        return new BufferStats(values.size(), mean, stdDev, Collections.min(values), Collections.max(values));
    }

    /**
     * 清理过期数据
     */
    private void cleanupExpiredData(String robotId, int jointIndex) {
        Map<Integer, Deque<TimestampedValue>> robotBuffers = buffers.get(robotId);
        if (robotBuffers == null) return;

        Deque<TimestampedValue> jointBuffer = robotBuffers.get(jointIndex);
        if (jointBuffer == null) return;

        Instant cutoff = Instant.now().minusSeconds(WINDOW_SIZE_SECONDS * 2); // 保留双倍窗口时间

        while (!jointBuffer.isEmpty() && jointBuffer.peekFirst().timestamp.isBefore(cutoff)) {
            jointBuffer.removeFirst();
        }
    }

    /**
     * 时间戳值包装类
     */
    private static class TimestampedValue {
        final double value;
        final Instant timestamp;

        TimestampedValue(double value, Instant timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }
    }

    /**
     * 缓冲区统计信息
     */
    public static class BufferStats {
        public final int count;
        public final double mean;
        public final double stdDev;
        public final double min;
        public final double max;

        public BufferStats(int count, double mean, double stdDev, double min, double max) {
            this.count = count;
            this.mean = mean;
            this.stdDev = stdDev;
            this.min = min;
            this.max = max;
        }
    }
}
