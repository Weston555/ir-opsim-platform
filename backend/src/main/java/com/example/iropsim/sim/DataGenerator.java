package com.example.iropsim.sim;

import com.example.iropsim.entity.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * 仿真数据生成器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataGenerator {

    private final Random random = new Random();
    private final ObjectMapper objectMapper;

    /**
     * 生成关节采样数据
     */
    public JointSample generateJointSample(Robot robot, int jointIndex, ScenarioRun scenarioRun,
                                         Instant timestamp, List<FaultInjection> activeFaults) {
        // 基础参数（从scenario中获取或使用默认值）
        double baseCurrent = 2.5; // 额定电流
        double baseTemp = 40.0;   // 额定温度
        double baseVibration = 0.1; // 额定振动

        JsonNode params = scenarioRun.getScenario().getBaseParams();
        if (params != null && !params.isNull()) {
            baseCurrent = params.has("current_nominal") ? params.get("current_nominal").asDouble(baseCurrent) : baseCurrent;
            baseTemp = params.has("temp_nominal") ? params.get("temp_nominal").asDouble(baseTemp) : baseTemp;
            baseVibration = params.has("vibration_nominal") ? params.get("vibration_nominal").asDouble(baseVibration) : baseVibration;
        }

        // 生成基础数据（添加随机噪声）
        double current = baseCurrent + (random.nextGaussian() * 0.1);
        double temperature = baseTemp + (random.nextGaussian() * 2.0);
        double vibration = Math.abs(baseVibration + (random.nextGaussian() * 0.02));

        // 检查是否有故障注入
        JointSample.SampleLabel label = JointSample.SampleLabel.NORMAL;
        for (FaultInjection fault : activeFaults) {
            if (isFaultActive(fault, timestamp)) {
                // 应用故障效果
                JsonNode faultParams = fault.getParams();
                if (faultParams != null && !faultParams.isNull()) {
                    switch (fault.getFaultType()) {
                            case OVERHEAT:
                                temperature += faultParams.has("amplitude") ? faultParams.get("amplitude").asDouble(10.0) : 10.0;
                                label = JointSample.SampleLabel.FAULT_OVERHEAT;
                                break;
                            case HIGH_VIBRATION:
                                vibration += faultParams.has("amplitude") ? faultParams.get("amplitude").asDouble(0.5) : 0.5;
                                label = JointSample.SampleLabel.FAULT_HIGH_VIBRATION;
                                break;
                            case CURRENT_SPIKE:
                                current += faultParams.has("amplitude") ? faultParams.get("amplitude").asDouble(2.0) : 2.0;
                                label = JointSample.SampleLabel.FAULT_CURRENT_SPIKE;
                                break;
                            case SENSOR_DRIFT:
                                // 传感器漂移：逐渐增加偏移
                                double driftRate = faultParams.has("drift_rate") ? faultParams.get("drift_rate").asDouble(0.01) : 0.01;
                                long elapsedSeconds = timestamp.getEpochSecond() - fault.getStartTs().getEpochSecond();
                                temperature += elapsedSeconds * driftRate;
                                label = JointSample.SampleLabel.FAULT_SENSOR_DRIFT;
                                break;
                        }
                }
            }
        }

        return JointSample.builder()
                .ts(timestamp)
                .robot(robot)
                .jointIndex(jointIndex)
                .currentA(Math.max(0, current))
                .temperatureC(Math.max(0, temperature))
                .vibrationRms(Math.max(0, vibration))
                .scenarioRun(scenarioRun)
                .label(label)
                .build();
    }

    /**
     * 生成位姿采样数据
     */
    public PoseSample generatePoseSample(Robot robot, ScenarioRun scenarioRun,
                                       Instant timestamp, List<FaultInjection> activeFaults) {
        // 生成基础位姿数据（模拟机器人运动）
        double x = 500 + (random.nextGaussian() * 50);  // 基础位置X
        double y = 300 + (random.nextGaussian() * 30);  // 基础位置Y
        double z = 200 + (random.nextGaussian() * 20);  // 基础位置Z

        // 随机旋转角度（弧度）
        double rx = random.nextDouble() * Math.PI * 2;
        double ry = random.nextDouble() * Math.PI * 2;
        double rz = random.nextDouble() * Math.PI * 2;

        // 检查是否有故障注入（目前位姿故障比较简单）
        PoseSample.SampleLabel label = PoseSample.SampleLabel.NORMAL;
        for (FaultInjection fault : activeFaults) {
            if (isFaultActive(fault, timestamp) && fault.getFaultType() == FaultInjection.FaultType.SENSOR_DRIFT) {
                // 位姿传感器漂移
                JsonNode faultParams = fault.getParams();
                if (faultParams != null && !faultParams.isNull()) {
                    double driftRate = faultParams.has("drift_rate") ? faultParams.get("drift_rate").asDouble(0.001) : 0.001;
                    long elapsedSeconds = timestamp.getEpochSecond() - fault.getStartTs().getEpochSecond();
                    x += elapsedSeconds * driftRate;
                    y += elapsedSeconds * driftRate;
                    label = PoseSample.SampleLabel.FAULT_SENSOR_DRIFT;
                }
            }
        }

        return PoseSample.builder()
                .ts(timestamp)
                .robot(robot)
                .x(x)
                .y(y)
                .z(z)
                .rx(rx)
                .ry(ry)
                .rz(rz)
                .scenarioRun(scenarioRun)
                .label(label)
                .build();
    }

    /**
     * 检查故障是否在激活时间窗口内
     */
    private boolean isFaultActive(FaultInjection fault, Instant timestamp) {
        return !timestamp.isBefore(fault.getStartTs()) && !timestamp.isAfter(fault.getEndTs());
    }

    /**
     * 设置随机种子以保证数据可复现
     */
    public void setSeed(long seed) {
        random.setSeed(seed);
    }
}
