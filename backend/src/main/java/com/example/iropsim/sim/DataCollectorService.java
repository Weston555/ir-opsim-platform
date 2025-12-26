package com.example.iropsim.sim;

import com.example.iropsim.entity.FaultInjection;
import com.example.iropsim.entity.JointSample;
import com.example.iropsim.entity.PoseSample;
import com.example.iropsim.entity.Robot;
import com.example.iropsim.entity.ScenarioRun;

import java.time.Instant;
import java.util.List;

/**
 * 数据采集服务接口 - 策略模式核心接口
 *
 * <p>该接口定义了数据采集的统一契约，支持两种数据采集策略：</p>
 * <ul>
 *   <li><b>模拟模式 (Simulation Mode)</b>: 使用数学模型生成逼真的机器人数据</li>
 *   <li><b>实时采集模式 (Real-time Acquisition Mode)</b>: 从真实设备采集数据</li>
 * </ul>
 *
 * <p><b>设计模式说明：</b></p>
 * <p>采用"策略模式(Strategy Pattern)"实现虚实数据源切换：
 * - {@link DataCollectorService} 是策略接口
 * - {@link SimulationCollector} 是模拟数据采集策略
 * - {@link RemoteDeviceCollector} 是真实设备数据采集策略
 * - {@link SimulationEngine} 作为上下文类，根据配置选择不同的策略</p>
 *
 * <p><b>架构优势：</b></p>
 * <ul>
 *   <li>解耦数据生成逻辑和仿真引擎</li>
 *   <li>支持运行时动态切换数据源</li>
 *   <li>便于扩展新的数据采集策略</li>
 *   <li>保持代码的可测试性和可维护性</li>
 * </ul>
 *
 * @author Industrial Robot Operations Simulation Platform
 * @version 2.0
 * @since 2024
 */
public interface DataCollectorService {

    /**
     * 数据源类型枚举
     */
    enum DataSourceType {
        /**
         * 模拟数据源：使用数学模型生成数据
         */
        SIMULATION,

        /**
         * 真实设备数据源：从物理设备实时采集
         */
        REAL_DEVICE
    }

    /**
     * 生成关节采样数据
     *
     * <p>根据指定的机器人关节、场景运行和时间戳生成传感器数据。
     * 在模拟模式下，使用正弦波函数模拟周期性运动加上随机噪声；
     * 在实时模式下，从外部数据源获取实际传感器读数。</p>
     *
     * @param robot 机器人实体
     * @param jointIndex 关节索引 (0-based)
     * @param scenarioRun 场景运行实例
     * @param timestamp 数据时间戳
     * @param activeFaults 当前激活的故障注入列表
     * @return 生成的关节采样数据
     */
    JointSample collectJointSample(Robot robot, int jointIndex, ScenarioRun scenarioRun,
                                   Instant timestamp, List<FaultInjection> activeFaults);

    /**
     * 生成位姿采样数据
     *
     * <p>生成机器人末端执行器的位姿信息，包括位置和姿态数据。
     * 模拟模式下使用随机游走模型，实时模式下从运动控制器获取实际位置。</p>
     *
     * @param robot 机器人实体
     * @param scenarioRun 场景运行实例
     * @param timestamp 数据时间戳
     * @param activeFaults 当前激活的故障注入列表
     * @return 生成的位姿采样数据
     */
    PoseSample collectPoseSample(Robot robot, ScenarioRun scenarioRun,
                                Instant timestamp, List<FaultInjection> activeFaults);

    /**
     * 获取当前数据源类型
     *
     * @return 数据源类型枚举值
     */
    DataSourceType getDataSourceType();

    /**
     * 检查数据采集服务是否可用
     *
     * <p>对于实时采集模式，检查设备连接状态；
     * 对于模拟模式，总是返回true。</p>
     *
     * @return 服务是否可用
     */
    boolean isAvailable();

    /**
     * 初始化数据采集服务
     *
     * <p>建立必要的连接、初始化缓存或准备数据生成器。
     * 应该在应用启动时或模式切换时调用。</p>
     */
    void initialize();

    /**
     * 关闭数据采集服务
     *
     * <p>释放资源、断开连接、清理缓存。
     * 应该在应用关闭时或模式切换时调用。</p>
     */
    void shutdown();
}
