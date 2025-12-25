package com.example.iropsim.detection;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 检测结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetectionResult {

    private boolean isAnomaly;
    private double score;
    private String severity; // INFO, WARN, CRITICAL
    private Object evidence; // 检测证据，如统计信息、阈值等
}
