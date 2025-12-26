package com.example.iropsim.sim;

import com.example.iropsim.entity.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Random;

/**
 * 模拟数据采集器 - 策略模式的具体策略实现
 *
 * <p>该类实现了{@link DataCollectorService}接口的模拟数据采集策略。
 * 使用优化后的数学模型生成逼真的工业机器人传感器数据，支持论文实验的可复现性要求。</p>
 *
 * <p><b>数据生成算法优化：</b></p>
 * <ul>
 *   <li><b>关节电流/位置：</b> 使用正弦波函数模拟机器人周期性往复运动，符合实际工业机器人运动规律</li>
 *   <li><b>噪声模型：</b> 添加高斯噪声模拟真实传感器的测量误差</li>
 *   <li><b>故障叠加：</b> 在基础正弦波上叠加故障效应，保持异常检测的准确性</li>
 *   <li><b>可复现性：</b> 支持随机种子设置，确保实验数据可重复生成</li>
 * </ul>
 *
 * <p><b>数学模型：</b></p>
 * <pre>{@code
 * current(t) = base_current + amplitude * sin(2π * frequency * t) + gaussian_noise
 * temperature(t) = base_temp + seasonal_variation + gaussian_noise + fault_effect
 * vibration(t) = base_vibration + motion_induced + gaussian_noise + fault_effect
 * }</pre>
 *
 * <p><b>运动周期模拟：</b></p>
 * <p>工业机器人通常执行重复的装配、焊接或搬运任务，运动具有明显的周期性。
 * 正弦波模型能够很好地模拟这种规律性运动，便于后续的周期性异常检测算法验证。</p>
 *
 * @author Industrial Robot Operations Simulation Platform
 * @version 2.0
 * @since 2024
 */
@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class SimulationCollector implements DataCollectorService {

    private final ObjectMapper objectMapper;
    private final Random random = new Random();

    // 运动参数常量 - 模拟典型的工业机器人运动特性
    private static final double MOTION_FREQUENCY = 0.1; // 0.1 Hz - 典型的机器人运动频率
    private static final double CURRENT_AMPLITUDE = 1.5; // 电流波动幅度 (A)
    private static final double VIBRATION_BASE = 0.08; // 基础振动水平 (RMS)
    private static final double TEMP_SEASONAL_AMPLITUDE = 3.0; // 温度季节性变化 (°C)


    @Override
    public JointSample collectJointSample(Robot robot, int jointIndex, ScenarioRun scenarioRun,
                                         Instant timestamp, List<FaultInjection> activeFaults) {

        // 基础参数配置
        double baseCurrent = 2.5; // 额定电流 (A)
        double baseTemp = 40.0;   // 额定温度 (°C)
        double baseVibration = VIBRATION_BASE;

        // 从场景配置读取参数
        JsonNode params = scenarioRun.getScenario().getBaseParams();
        if (params != null && !params.isNull()) {
            baseCurrent = params.has("current_nominal") ? params.get("current_nominal").asDouble(baseCurrent) : baseCurrent;
            baseTemp = params.has("temp_nominal") ? params.get("temp_nominal").asDouble(baseTemp) : baseTemp;
            baseVibration = params.has("vibration_nominal") ? params.get("vibration_nominal").asDouble(baseVibration) : baseVibration;
        }

        // 时间相关的正弦波基础值 - 模拟周期性运动
        long epochSecond = timestamp.getEpochSecond();
        double timeRadians = 2 * Math.PI * MOTION_FREQUENCY * epochSecond;

        // 关节特定的相位偏移 - 模拟多关节协调运动
        double jointPhase = jointIndex * Math.PI / 3.0; // 每个关节相差60度
        double motionFactor = Math.sin(timeRadians + jointPhase);

        // 生成基础数据 - 正弦波 + 噪声
        double current = baseCurrent + CURRENT_AMPLITUDE * motionFactor + (random.nextGaussian() * 0.15);
        double temperature = baseTemp + TEMP_SEASONAL_AMPLITUDE * Math.sin(2 * Math.PI * 0.01 * epochSecond) + (random.nextGaussian() * 1.5);
        double vibration = Math.abs(baseVibration + 0.05 * Math.abs(motionFactor) + (random.nextGaussian() * 0.01));

        // 应用故障注入
        JointSample.SampleLabel label = JointSample.SampleLabel.NORMAL;
        for (FaultInjection fault : activeFaults) {
            if (isFaultActive(fault, timestamp)) {
                JsonNode faultParams = fault.getParams();
                if (faultParams != null && !faultParams.isNull()) {
                    switch (fault.getFaultType()) {
                        case OVERHEAT:
                            double heatAmplitude = faultParams.has("amplitude") ? faultParams.get("amplitude").asDouble(10.0) : 10.0;
                            temperature += heatAmplitude * Math.sin(timeRadians * 2); // 故障温度也具有波动性
                            label = JointSample.SampleLabel.FAULT_OVERHEAT;
                            break;

                        case HIGH_VIBRATION:
                            double vibAmplitude = faultParams.has("amplitude") ? faultParams.get("amplitude").asDouble(0.5) : 0.5;
                            vibration += vibAmplitude + 0.1 * Math.sin(timeRadians * 5); // 高频振动
                            label = JointSample.SampleLabel.FAULT_HIGH_VIBRATION;
                            break;

                        case CURRENT_SPIKE:
                            double currentAmplitude = faultParams.has("amplitude") ? faultParams.get("amplitude").asDouble(2.0) : 2.0;
                            current += currentAmplitude * (1 + Math.sin(timeRadians * 3)); // 脉冲式电流异常
                            label = JointSample.SampleLabel.FAULT_CURRENT_SPIKE;
                            break;

                        case SENSOR_DRIFT:
                            double driftRate = faultParams.has("driftRate") ? faultParams.get("driftRate").asDouble(0.01) : 0.01;
                            long elapsedSeconds = epochSecond - fault.getStartTs().getEpochSecond();
                            // 渐进式漂移 + 随机游走
                            double drift = elapsedSeconds * driftRate + random.nextGaussian() * 0.005;
                            temperature += drift;
                            vibration += Math.abs(drift) * 0.1;
                            label = JointSample.SampleLabel.FAULT_SENSOR_DRIFT;
                            break;
                    }
                }
            }
        }

        // 确保物理合理性约束
        current = Math.max(0, current);
        temperature = Math.max(0, temperature);
        vibration = Math.max(0, vibration);

        return JointSample.builder()
                .ts(timestamp)
                .robot(robot)
                .jointIndex(jointIndex)
                .currentA(current)
                .temperatureC(temperature)
                .vibrationRms(vibration)
                .scenarioRun(scenarioRun)
                .label(label)
                .build();
    }

    @Override
    public PoseSample collectPoseSample(Robot robot, ScenarioRun scenarioRun,
                                       Instant timestamp, List<FaultInjection> activeFaults) {

        long epochSecond = timestamp.getEpochSecond();
        double timeRadians = 2 * Math.PI * MOTION_FREQUENCY * epochSecond;

        // 模拟机器人工作空间内的运动轨迹
        // 使用多个频率分量模拟复杂的工业操作轨迹
        double x = 500 + 100 * Math.sin(timeRadians) + 30 * Math.sin(timeRadians * 3) + random.nextGaussian() * 5;
        double y = 300 + 80 * Math.cos(timeRadians) + 20 * Math.cos(timeRadians * 2.5) + random.nextGaussian() * 3;
        double z = 200 + 50 * Math.sin(timeRadians * 1.5) + random.nextGaussian() * 2;

        // 姿态角度 - 模拟末端执行器的定向变化
        double rx = Math.PI/4 + 0.5 * Math.sin(timeRadians * 2) + random.nextGaussian() * 0.1;
        double ry = 0.3 * Math.cos(timeRadians * 1.8) + random.nextGaussian() * 0.05;
        double rz = Math.PI/6 + 0.4 * Math.sin(timeRadians * 2.2) + random.nextGaussian() * 0.08;

        PoseSample.SampleLabel label = PoseSample.SampleLabel.NORMAL;

        // 应用故障效应
        for (FaultInjection fault : activeFaults) {
            if (isFaultActive(fault, timestamp) && fault.getFaultType() == FaultInjection.FaultType.SENSOR_DRIFT) {
                JsonNode faultParams = fault.getParams();
                if (faultParams != null && !faultParams.isNull()) {
                    double driftRate = faultParams.has("driftRate") ? faultParams.get("driftRate").asDouble(0.001) : 0.001;
                    long elapsedSeconds = epochSecond - fault.getStartTs().getEpochSecond();

                    // 位置传感器漂移 - 渐进式误差积累
                    double positionDrift = elapsedSeconds * driftRate;
                    x += positionDrift + random.nextGaussian() * 0.5;
                    y += positionDrift + random.nextGaussian() * 0.3;
                    z += positionDrift * 0.5 + random.nextGaussian() * 0.2;

                    // 姿态传感器漂移
                    rx += elapsedSeconds * driftRate * 0.1;
                    ry += elapsedSeconds * driftRate * 0.05;
                    rz += elapsedSeconds * driftRate * 0.08;

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

    @Override
    public DataSourceType getDataSourceType() {
        return DataSourceType.SIMULATION;
    }

    @Override
    public boolean isAvailable() {
        return true; // 模拟模式总是可用
    }

    @Override
    public void initialize() {
        log.info("Initializing Simulation Data Collector with optimized sinusoidal models");
        // 可以在这里预热随机数生成器或初始化缓存
    }

    @Override
    public void shutdown() {
        log.info("Shutting down Simulation Data Collector");
        // 清理资源
    }

    /**
     * 设置随机种子以保证数据可复现
     *
     * <p>论文实验要求数据可复现，通过设置种子确保每次运行生成相同的数据序列。
     * 这对于验证异常检测算法的稳定性和对比不同算法性能至关重要。</p>
     *
     * @param seed 随机种子
     */
    public void setSeed(long seed) {
        random.setSeed(seed);
        log.debug("Set random seed for reproducible data generation: {}", seed);
    }

    /**
     * 检查故障是否在激活时间窗口内
     */
    private boolean isFaultActive(FaultInjection fault, Instant timestamp) {
        return !timestamp.isBefore(fault.getStartTs()) && !timestamp.isAfter(fault.getEndTs());
    }
}
