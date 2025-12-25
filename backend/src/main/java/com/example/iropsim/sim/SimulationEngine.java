package com.example.iropsim.sim;

import com.example.iropsim.detection.AnomalyDetectionService;
import com.example.iropsim.entity.*;
import com.example.iropsim.repository.*;
import com.example.iropsim.websocket.WebSocketEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 仿真引擎
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SimulationEngine {

    private final ScenarioRunRepository scenarioRunRepository;
    private final RobotRepository robotRepository;
    private final JointSampleRepository jointSampleRepository;
    private final PoseSampleRepository poseSampleRepository;
    private final FaultInjectionRepository faultInjectionRepository;
    private final DataGenerator dataGenerator;
    private final ScheduledExecutorService scheduledExecutor;
    private final WebSocketEventHandler webSocketEventHandler;
    private final AnomalyDetectionService anomalyDetectionService;

    // 运行中的仿真任务
    private final Map<UUID, ScheduledFuture<?>> runningSimulations = new ConcurrentHashMap<>();

    /**
     * 启动仿真运行
     */
    public void startSimulation(UUID runId) {
        ScenarioRun scenarioRun = scenarioRunRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Scenario run not found: " + runId));

        if (scenarioRun.getStatus() == ScenarioRun.RunStatus.RUNNING) {
            throw new IllegalStateException("Simulation is already running");
        }

        // 设置随机种子保证可复现
        dataGenerator.setSeed(scenarioRun.getSeed());

        // 获取机器人列表（暂时只支持一个机器人）
        List<Robot> robots = robotRepository.findAll();
        if (robots.isEmpty()) {
            throw new IllegalStateException("No robots available for simulation");
        }

        // 更新状态
        scenarioRun.setStatus(ScenarioRun.RunStatus.RUNNING);
        scenarioRun.setStartedAt(Instant.now());
        scenarioRun = scenarioRunRepository.save(scenarioRun);

        // 推送状态更新
        webSocketEventHandler.pushSimulationStatus(runId, scenarioRun);

        // 启动定时任务
        ScheduledFuture<?> future = scheduledExecutor.scheduleAtFixedRate(
                new SimulationTask(runId, robots.get(0)),
                0,
                1000 / scenarioRun.getRateHz(), // 转换为毫秒间隔
                TimeUnit.MILLISECONDS
        );

        runningSimulations.put(runId, future);
        log.info("Started simulation for run: {}", runId);
    }

    /**
     * 停止仿真运行
     */
    public void stopSimulation(UUID runId) {
        ScheduledFuture<?> future = runningSimulations.remove(runId);
        if (future != null) {
            future.cancel(false);

            // 更新数据库状态
            ScenarioRun scenarioRun = scenarioRunRepository.findById(runId).orElse(null);
            if (scenarioRun != null) {
                scenarioRun.setStatus(ScenarioRun.RunStatus.STOPPED);
                scenarioRun.setEndedAt(Instant.now());
                scenarioRun = scenarioRunRepository.save(scenarioRun);

                // 推送状态更新
                webSocketEventHandler.pushSimulationStatus(runId, scenarioRun);
            }

            log.info("Stopped simulation for run: {}", runId);
        }
    }

    /**
     * 获取仿真状态
     */
    public ScenarioRun getSimulationStatus(UUID runId) {
        return scenarioRunRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Scenario run not found: " + runId));
    }

    /**
     * 检查仿真是否正在运行
     */
    public boolean isSimulationRunning(UUID runId) {
        ScheduledFuture<?> future = runningSimulations.get(runId);
        return future != null && !future.isDone();
    }

    /**
     * 仿真任务内部类
     */
    private class SimulationTask implements Runnable {
        private final UUID runId;
        private final Robot robot;

        public SimulationTask(UUID runId, Robot robot) {
            this.runId = runId;
            this.robot = robot;
        }

        @Override
        public void run() {
            try {
                ScenarioRun scenarioRun = scenarioRunRepository.findById(runId).orElse(null);
                if (scenarioRun == null || scenarioRun.getStatus() != ScenarioRun.RunStatus.RUNNING) {
                    // 仿真已停止，清理任务
                    runningSimulations.remove(runId);
                    return;
                }

                Instant now = Instant.now();

                // 获取当前激活的故障注入
                List<FaultInjection> activeFaults = faultInjectionRepository
                        .findByScenarioRunIdAndTimeRange(runId, now);

                // 生成关节数据（每个关节）
                for (int jointIndex = 0; jointIndex < robot.getJointCount(); jointIndex++) {
                    JointSample jointSample = dataGenerator.generateJointSample(
                            robot, jointIndex, scenarioRun, now, activeFaults);
                    jointSample = jointSampleRepository.save(jointSample);

                    // 执行异常检测
                    anomalyDetectionService.processSample(jointSample);
                }

                // 生成位姿数据
                PoseSample poseSample = dataGenerator.generatePoseSample(
                        robot, scenarioRun, now, activeFaults);
                poseSample = poseSampleRepository.save(poseSample);

                // 执行位姿异常检测
                anomalyDetectionService.processSample(poseSample);

                // 推送最新的传感器数据到WebSocket客户端
                List<JointSample> latestJointSamples = jointSampleRepository.findAll().stream()
                        .filter(sample -> sample.getRobot().getId().equals(robot.getId()))
                        .collect(java.util.stream.Collectors.groupingBy(
                                JointSample::getJointIndex,
                                java.util.stream.Collectors.maxBy(java.util.Comparator.comparing(JointSample::getTs))
                        ))
                        .values()
                        .stream()
                        .filter(java.util.Optional::isPresent)
                        .map(java.util.Optional::get)
                        .collect(java.util.stream.Collectors.toList());

                webSocketEventHandler.pushLatestSensorData(robot.getId(), latestJointSamples, poseSample);

                log.debug("Generated simulation data for run: {} at time: {}", runId, now);

            } catch (Exception e) {
                log.error("Error in simulation task for run: {}", runId, e);
                // 发生错误时停止仿真
                stopSimulation(runId);
            }
        }
    }
}
