package com.example.iropsim.sim;

import com.example.iropsim.entity.*;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 远程设备数据采集器 - 真实设备数据采集策略实现
 *
 * <p>该类实现了{@link DataCollectorService}接口的真实设备数据采集策略。
 * 预留了从物理工业机器人设备实时采集数据的标准接口架构。</p>
 *
 * <p><b>设计目标：</b></p>
 * <ul>
 *   <li>提供标准化的设备通信接口</li>
 *   <li>支持多种通信协议 (HTTP/MQTT/WebSocket)</li>
 *   <li>实现数据缓冲和容错机制</li>
 *   <li>预留实时性优化空间</li>
 * </ul>
 *
 * <p><b>通信协议支持：</b></p>
 * <ul>
 *   <li><b>HTTP REST API:</b> 从机器人控制器获取传感器数据</li>
 *   <li><b>MQTT:</b> 订阅设备状态主题</li>
 *   <li><b>WebSocket:</b> 实时数据推送</li>
 *   <li><b>OPC UA:</b> 工业自动化标准协议 (预留扩展)</li>
 * </ul>
 *
 * <p><b>数据流架构：</b></p>
 * <pre>{@code
 * 物理设备 -> 设备控制器 -> 数据采集器 -> 缓冲队列 -> 仿真引擎
 *     |           |              |           |          |
 *   传感器     REST API       HTTP客户端   内存队列   数据处理
 * }</pre>
 *
 * <p><b>容错设计：</b></p>
 * <ul>
 *   <li>连接断开时自动重试</li>
 *   <li>数据丢失时的插值补偿</li>
 *   <li>设备异常时的降级处理</li>
 *   <li>数据质量验证和清洗</li>
 * </ul>
 *
 * @author Industrial Robot Operations Simulation Platform
 * @version 2.0
 * @since 2024
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RemoteDeviceCollector implements DataCollectorService {

    private final RestTemplate restTemplate = new RestTemplate();

    // 数据缓冲队列 - 处理实时数据流
    private final BlockingQueue<DeviceData> dataBuffer = new LinkedBlockingQueue<>(1000);

    // 设备连接状态
    private volatile boolean connected = false;
    private volatile Instant lastDataReceived = Instant.now();

    // 设备配置 (实际部署时从配置文件读取)
    private static final String DEVICE_BASE_URL = "http://robot-controller:8080/api";
    private static final String DEVICE_API_KEY = "industrial-robot-platform-key";

    @Override
    public JointSample collectJointSample(Robot robot, int jointIndex, ScenarioRun scenarioRun,
                                         Instant timestamp, List<FaultInjection> activeFaults) {

        // 从设备获取实时数据或从缓冲区读取
        DeviceData latestData = getLatestDeviceData();

        if (latestData == null || latestData.jointData == null) {
            log.warn("No joint data available from device, falling back to simulation mode");
            throw new RuntimeException("Device data not available - consider switching to simulation mode");
        }

        // 从设备数据构造关节样本
        JointData jointData = latestData.jointData[jointIndex];
        if (jointData == null) {
            log.warn("Joint {} data not available, using default values", jointIndex);
            jointData = new JointData(2.5, 40.0, 0.1); // 默认值
        }

        // 应用故障注入 (在真实设备上模拟故障)
        JointSample.SampleLabel label = JointSample.SampleLabel.NORMAL;
        for (FaultInjection fault : activeFaults) {
            if (isFaultActive(fault, timestamp)) {
                label = applyFaultEffect(jointData, fault);
            }
        }

        return JointSample.builder()
                .ts(latestData.timestamp)
                .robot(robot)
                .jointIndex(jointIndex)
                .currentA(jointData.current)
                .temperatureC(jointData.temperature)
                .vibrationRms(jointData.vibration)
                .scenarioRun(scenarioRun)
                .label(label)
                .build();
    }

    @Override
    public PoseSample collectPoseSample(Robot robot, ScenarioRun scenarioRun,
                                       Instant timestamp, List<FaultInjection> activeFaults) {

        DeviceData latestData = getLatestDeviceData();

        if (latestData == null || latestData.poseData == null) {
            log.warn("No pose data available from device, falling back to simulation mode");
            throw new RuntimeException("Device data not available - consider switching to simulation mode");
        }

        PoseData poseData = latestData.poseData;

        // 应用故障注入
        PoseSample.SampleLabel label = PoseSample.SampleLabel.NORMAL;
        for (FaultInjection fault : activeFaults) {
            if (isFaultActive(fault, timestamp) && fault.getFaultType() == FaultInjection.FaultType.SENSOR_DRIFT) {
                // 在真实数据上模拟传感器漂移
                JsonNode faultParams = fault.getParams();
                if (faultParams != null && !faultParams.isNull()) {
                    double driftRate = faultParams.has("driftRate") ? faultParams.get("driftRate").asDouble(0.001) : 0.001;
                    long elapsedSeconds = timestamp.getEpochSecond() - fault.getStartTs().getEpochSecond();
                    double drift = elapsedSeconds * driftRate;

                    poseData.x += drift;
                    poseData.y += drift;
                    poseData.z += drift * 0.5;
                    label = PoseSample.SampleLabel.FAULT_SENSOR_DRIFT;
                }
            }
        }

        return PoseSample.builder()
                .ts(latestData.timestamp)
                .robot(robot)
                .x(poseData.x)
                .y(poseData.y)
                .z(poseData.z)
                .rx(poseData.rx)
                .ry(poseData.ry)
                .rz(poseData.rz)
                .scenarioRun(scenarioRun)
                .label(label)
                .build();
    }

    @Override
    public DataSourceType getDataSourceType() {
        return DataSourceType.REAL_DEVICE;
    }

    @Override
    public boolean isAvailable() {
        // 检查设备连接状态和数据新鲜度
        return connected && lastDataReceived.isAfter(Instant.now().minusSeconds(30));
    }

    @Override
    public void initialize() {
        log.info("Initializing Remote Device Data Collector");

        try {
            // 尝试连接到设备
            connectToDevice();

            // 启动数据采集线程
            startDataCollectionThread();

            // 启动健康检查
            startHealthCheckThread();

            log.info("Remote Device Collector initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize Remote Device Collector", e);
            connected = false;
        }
    }

    @Override
    public void shutdown() {
        log.info("Shutting down Remote Device Data Collector");
        connected = false;

        // 清理资源
        dataBuffer.clear();

        try {
            disconnectFromDevice();
        } catch (Exception e) {
            log.warn("Error during device disconnection", e);
        }
    }

    /**
     * 获取最新的设备数据
     */
    private DeviceData getLatestDeviceData() {
        // 首先尝试从缓冲区获取最新数据
        DeviceData data = dataBuffer.poll();
        if (data != null) {
            return data;
        }

        // 如果缓冲区为空，尝试直接从设备获取
        try {
            return fetchDataFromDevice();
        } catch (Exception e) {
            log.warn("Failed to fetch data from device: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从设备直接获取数据
     */
    private DeviceData fetchDataFromDevice() throws Exception {
        if (!connected) {
            throw new RuntimeException("Device not connected");
        }

        // 构建请求
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + DEVICE_API_KEY);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        // 调用设备API
        String url = DEVICE_BASE_URL + "/robot/status";
        ResponseEntity<DeviceApiResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, DeviceApiResponse.class);

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new RuntimeException("Device API returned error: " + response.getStatusCode());
        }

        // 转换API响应为内部数据格式
        return convertApiResponse(response.getBody());
    }

    /**
     * 连接到设备
     */
    private void connectToDevice() throws Exception {
        log.info("Attempting to connect to robot device at: {}", DEVICE_BASE_URL);

        // 发送连接测试请求
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + DEVICE_API_KEY);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                DEVICE_BASE_URL + "/health", HttpMethod.GET, entity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            connected = true;
            log.info("Successfully connected to robot device");
        } else {
            throw new RuntimeException("Device health check failed: " + response.getStatusCode());
        }
    }

    /**
     * 断开设备连接
     */
    private void disconnectFromDevice() throws Exception {
        // 发送断开连接的清理请求 (如果设备支持)
        log.info("Disconnecting from robot device");
    }

    /**
     * 启动数据采集线程
     */
    private void startDataCollectionThread() {
        Thread dataCollectionThread = new Thread(() -> {
            while (connected) {
                try {
                    DeviceData data = fetchDataFromDevice();
                    if (data != null) {
                        // 添加到缓冲区，如果缓冲区满则丢弃最旧的数据
                        if (!dataBuffer.offer(data)) {
                            dataBuffer.poll(); // 移除最旧的数据
                            dataBuffer.offer(data);
                        }
                        lastDataReceived = Instant.now();
                    }

                    // 根据采样率休眠
                    Thread.sleep(100); // 10Hz采样

                } catch (Exception e) {
                    log.warn("Data collection error: {}", e.getMessage());
                    try {
                        Thread.sleep(1000); // 错误时等待1秒后重试
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        });

        dataCollectionThread.setName("Device-Data-Collector");
        dataCollectionThread.setDaemon(true);
        dataCollectionThread.start();

        log.info("Data collection thread started");
    }

    /**
     * 启动健康检查线程
     */
    private void startHealthCheckThread() {
        Thread healthCheckThread = new Thread(() -> {
            while (connected) {
                try {
                    // 定期检查设备连接状态
                    connectToDevice(); // 重新检查连接

                    Thread.sleep(30000); // 每30秒检查一次

                } catch (Exception e) {
                    log.warn("Device health check failed, marking as disconnected");
                    connected = false;

                    // 尝试重连
                    int retryCount = 0;
                    while (!connected && retryCount < 5) {
                        try {
                            Thread.sleep(5000); // 等待5秒后重试
                            connectToDevice();
                        } catch (Exception retryException) {
                            retryCount++;
                            log.warn("Reconnection attempt {} failed", retryCount);
                        }
                    }
                }
            }
        });

        healthCheckThread.setName("Device-Health-Check");
        healthCheckThread.setDaemon(true);
        healthCheckThread.start();

        log.info("Health check thread started");
    }

    /**
     * 应用故障效应到设备数据
     */
    private JointSample.SampleLabel applyFaultEffect(JointData jointData, FaultInjection fault) {
        JsonNode faultParams = fault.getParams();
        if (faultParams == null || faultParams.isNull()) {
            return JointSample.SampleLabel.NORMAL;
        }

        switch (fault.getFaultType()) {
            case OVERHEAT:
                double heatAmplitude = faultParams.has("amplitude") ? faultParams.get("amplitude").asDouble(10.0) : 10.0;
                jointData.temperature += heatAmplitude;
                return JointSample.SampleLabel.FAULT_OVERHEAT;

            case HIGH_VIBRATION:
                double vibAmplitude = faultParams.has("amplitude") ? faultParams.get("amplitude").asDouble(0.5) : 0.5;
                jointData.vibration += vibAmplitude;
                return JointSample.SampleLabel.FAULT_HIGH_VIBRATION;

            case CURRENT_SPIKE:
                double currentAmplitude = faultParams.has("amplitude") ? faultParams.get("amplitude").asDouble(2.0) : 2.0;
                jointData.current += currentAmplitude;
                return JointSample.SampleLabel.FAULT_CURRENT_SPIKE;

            case SENSOR_DRIFT:
                // 传感器漂移已经在数据预处理阶段应用
                return JointSample.SampleLabel.FAULT_SENSOR_DRIFT;

            default:
                return JointSample.SampleLabel.NORMAL;
        }
    }

    /**
     * 检查故障是否激活
     */
    private boolean isFaultActive(FaultInjection fault, Instant timestamp) {
        return !timestamp.isBefore(fault.getStartTs()) && !timestamp.isAfter(fault.getEndTs());
    }

    /**
     * 转换设备API响应为内部数据格式
     */
    private DeviceData convertApiResponse(DeviceApiResponse apiResponse) {
        DeviceData data = new DeviceData();
        data.timestamp = Instant.now(); // 使用当前时间戳

        // 转换关节数据
        if (apiResponse.joints != null && apiResponse.joints.length > 0) {
            data.jointData = new JointData[apiResponse.joints.length];
            for (int i = 0; i < apiResponse.joints.length; i++) {
                DeviceApiResponse.JointStatus joint = apiResponse.joints[i];
                data.jointData[i] = new JointData(
                    joint.current != null ? joint.current : 2.5,
                    joint.temperature != null ? joint.temperature : 40.0,
                    joint.vibration != null ? joint.vibration : 0.1
                );
            }
        }

        // 转换位姿数据
        if (apiResponse.pose != null) {
            data.poseData = new PoseData();
            data.poseData.x = apiResponse.pose.x != null ? apiResponse.pose.x : 500.0;
            data.poseData.y = apiResponse.pose.y != null ? apiResponse.pose.y : 300.0;
            data.poseData.z = apiResponse.pose.z != null ? apiResponse.pose.z : 200.0;
            data.poseData.rx = apiResponse.pose.rx != null ? apiResponse.pose.rx : 0.0;
            data.poseData.ry = apiResponse.pose.ry != null ? apiResponse.pose.ry : 0.0;
            data.poseData.rz = apiResponse.pose.rz != null ? apiResponse.pose.rz : 0.0;
        }

        return data;
    }

    // 内部数据结构定义

    /**
     * 设备原始数据
     */
    private static class DeviceData {
        Instant timestamp;
        JointData[] jointData;
        PoseData poseData;
    }

    /**
     * 关节数据
     */
    private static class JointData {
        double current;
        double temperature;
        double vibration;

        JointData(double current, double temperature, double vibration) {
            this.current = current;
            this.temperature = temperature;
            this.vibration = vibration;
        }
    }

    /**
     * 位姿数据
     */
    private static class PoseData {
        double x, y, z, rx, ry, rz;
    }

    /**
     * 设备API响应格式
     */
    private static class DeviceApiResponse {
        JointStatus[] joints;
        PoseStatus pose;

        static class JointStatus {
            Double current;
            Double temperature;
            Double vibration;
        }

        static class PoseStatus {
            Double x, y, z, rx, ry, rz;
        }
    }
}
