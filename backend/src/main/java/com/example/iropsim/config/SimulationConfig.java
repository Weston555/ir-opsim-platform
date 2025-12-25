package com.example.iropsim.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 仿真配置
 */
@Configuration
public class SimulationConfig {

    @Bean
    public ScheduledExecutorService scheduledExecutor() {
        // 使用固定大小的线程池来执行仿真任务
        return Executors.newScheduledThreadPool(10);
    }
}
