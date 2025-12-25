package com.example.iropsim.websocket;

import com.example.iropsim.entity.JointSample;
import com.example.iropsim.entity.PoseSample;
import com.example.iropsim.entity.ScenarioRun;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * WebSocket事件处理器
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketEventHandler {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 推送最新的传感器数据
     */
    public void pushLatestSensorData(UUID robotId, List<JointSample> jointSamples, PoseSample poseSample) {
        try {
            LatestSensorData data = new LatestSensorData(jointSamples, poseSample);
            String destination = "/topic/robots/" + robotId + "/latest";
            messagingTemplate.convertAndSend(destination, data);
            log.debug("Pushed latest sensor data for robot: {}", robotId);
        } catch (Exception e) {
            log.error("Failed to push latest sensor data for robot: {}", robotId, e);
        }
    }

    /**
     * 推送仿真运行状态更新
     */
    public void pushSimulationStatus(UUID runId, ScenarioRun scenarioRun) {
        try {
            String destination = "/topic/sim/runs/" + runId + "/status";
            messagingTemplate.convertAndSend(destination, scenarioRun);
            log.debug("Pushed simulation status for run: {}", runId);
        } catch (Exception e) {
            log.error("Failed to push simulation status for run: {}", runId, e);
        }
    }

    /**
     * 推送告警事件
     */
    public void pushAlarmEvent(Object alarmEvent) {
        try {
            messagingTemplate.convertAndSend("/topic/alarms", alarmEvent);
            log.debug("Pushed alarm event");
        } catch (Exception e) {
            log.error("Failed to push alarm event", e);
        }
    }

    /**
     * 最新传感器数据DTO
     */
    public static class LatestSensorData {
        private final List<JointSample> jointSamples;
        private final PoseSample poseSample;

        public LatestSensorData(List<JointSample> jointSamples, PoseSample poseSample) {
            this.jointSamples = jointSamples;
            this.poseSample = poseSample;
        }

        public List<JointSample> getJointSamples() {
            return jointSamples;
        }

        public PoseSample getPoseSample() {
            return poseSample;
        }
    }
}
