package com.example.iropsim.config;

import com.example.iropsim.sim.DataCollectorService;
import com.example.iropsim.sim.SimulationCollector;
import com.example.iropsim.sim.RemoteDeviceCollector;
import com.example.iropsim.sim.SimulationEngine;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import jakarta.annotation.PostConstruct;

/**
 * 数据采集配置类
 *
 * <p>配置数据采集策略模式的核心组件：</p>
 * <ul>
 *   <li>{@link SimulationCollector} - 模拟数据采集器（默认）</li>
 *   <li>{@link RemoteDeviceCollector} - 远程设备数据采集器</li>
 *   <li>默认数据源设置为模拟模式</li>
 * </ul>
 *
 * <p><b>依赖注入策略：</b></p>
 * <p>使用Spring的@Primary注解设置默认的数据采集器为模拟模式，
 * 支持运行时通过{@link SimulationEngine#setDataCollector(DataCollectorService)}
 * 动态切换到真实设备采集器。</p>
 *
 * @author Industrial Robot Operations Simulation Platform
 * @version 2.0
 * @since 2024
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class DataCollectionConfig {

    private final ObjectMapper objectMapper;

    /**
     * 配置模拟数据采集器
     *
     * <p>创建基于正弦波数学模型的模拟数据采集器，作为默认的数据源。
     * 该采集器使用优化后的算法生成符合工业机器人运动规律的数据。</p>
     *
     * @return 模拟数据采集器实例
     */
    @Bean
    @Primary
    public DataCollectorService simulationCollector() {
        return new SimulationCollector(objectMapper);
    }

    /**
     * 配置远程设备数据采集器
     *
     * <p>创建用于从真实工业机器人设备采集数据的采集器。
     * 该采集器预留了HTTP REST API、MQTT和WebSocket等通信接口。</p>
     *
     * @return 远程设备数据采集器实例
     */
    @Bean
    public DataCollectorService remoteDeviceCollector() {
        return new RemoteDeviceCollector();
    }

    /**
     * 初始化数据采集配置
     *
     * <p>在应用启动时初始化默认的数据采集器，并记录配置信息。</p>
     */
    @PostConstruct
    public void initialize() {
        log.info("Initializing Data Collection Configuration");
        log.info("Available data sources:");
        log.info("  - SIMULATION: SimulationCollector (default)");
        log.info("  - REAL_DEVICE: RemoteDeviceCollector (requires device connection)");
        log.info("Use POST /api/v1/sim/datasource/switch to change data source at runtime");
    }
}
