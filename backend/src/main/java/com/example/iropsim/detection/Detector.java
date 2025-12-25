package com.example.iropsim.detection;

import java.util.List;

/**
 * 异常检测器接口
 */
public interface Detector<T> {

    /**
     * 执行异常检测
     * @param values 待检测的值序列
     * @return 检测结果
     */
    DetectionResult detect(List<T> values);

    /**
     * 获取检测器类型
     */
    String getType();
}
