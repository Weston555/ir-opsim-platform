<template>
  <div class="robot-detail-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="robot-info">
        <h1>{{ robot?.name || '机器人详情' }}</h1>
        <div class="robot-meta">
          <el-tag>{{ robot?.model }}</el-tag>
          <span>{{ robot?.jointCount }}个关节</span>
        </div>
      </div>
      <div class="page-actions">
        <el-button @click="$router.back()">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
      </div>
    </div>

    <!-- 实时状态卡片 -->
    <div class="status-cards" v-if="telemetryData">
      <el-card class="status-card">
        <div class="status-header">
          <el-icon size="24" color="#409EFF"><Lightning /></el-icon>
          <span>电流监控</span>
        </div>
        <div class="status-value">
          <span class="value">{{ currentValue?.toFixed(2) || '--' }}</span>
          <span class="unit">A</span>
        </div>
      </el-card>

      <el-card class="status-card">
        <div class="status-header">
          <el-icon size="24" color="#E6A23C"><Warning /></el-icon>
          <span>振动监控</span>
        </div>
        <div class="status-value">
          <span class="value">{{ vibrationValue?.toFixed(3) || '--' }}</span>
          <span class="unit">RMS</span>
        </div>
      </el-card>

      <el-card class="status-card">
        <div class="status-header">
          <el-icon size="24" color="#F56C6C"><HotWater /></el-icon>
          <span>温度监控</span>
        </div>
        <div class="status-value">
          <span class="value">{{ temperatureValue?.toFixed(1) || '--' }}</span>
          <span class="unit">°C</span>
        </div>
      </el-card>
    </div>

    <!-- 图表区域 -->
    <div class="charts-section">
      <!-- 电流趋势图 -->
      <el-card class="chart-card">
        <template #header>
          <div class="chart-header">
            <span>电流趋势图</span>
            <el-select v-model="timeRange" placeholder="时间范围" size="small" @change="updateCharts">
              <el-option label="最近5分钟" value="5m" />
              <el-option label="最近15分钟" value="15m" />
              <el-option label="最近30分钟" value="30m" />
            </el-select>
          </div>
        </template>
        <div ref="currentChartRef" class="chart-container"></div>
      </el-card>

      <!-- 振动趋势图 -->
      <el-card class="chart-card">
        <template #header>
          <div class="chart-header">
            <span>振动趋势图</span>
          </div>
        </template>
        <div ref="vibrationChartRef" class="chart-container"></div>
      </el-card>

      <!-- 温度趋势图 -->
      <el-card class="chart-card">
        <template #header>
          <div class="chart-header">
            <span>温度趋势图</span>
          </div>
        </template>
        <div ref="temperatureChartRef" class="chart-container"></div>
      </el-card>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <el-icon class="is-loading">
        <Loading />
      </el-icon>
      <span>加载中...</span>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="error-state">
      <el-alert
        title="加载失败"
        :description="error"
        type="error"
        show-icon
      />
      <el-button @click="loadData" style="margin-top: 16px;">重试</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft,
  Lightning,
  Warning,
  HotWater,
  Loading
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { useRobotStore } from '@/stores/robot'
import type { TelemetryData } from '@/types/robot'

const route = useRoute()
const robotStore = useRobotStore()

// 状态
const robotId = route.params.id as string
const loading = ref(false)
const error = ref('')
const timeRange = ref('15m')
const telemetryData = ref<TelemetryData | null>(null)

// 图表实例
const currentChartRef = ref<HTMLDivElement>()
const vibrationChartRef = ref<HTMLDivElement>()
const temperatureChartRef = ref<HTMLDivElement>()
let currentChart: echarts.ECharts | null = null
let vibrationChart: echarts.ECharts | null = null
let temperatureChart: echarts.ECharts | null = null

// 计算属性
const robot = computed(() => robotStore.currentRobot)
const currentValue = computed(() => {
  if (!telemetryData.value?.jointSamples?.length) return null
  return telemetryData.value.jointSamples[0].currentA
})
const vibrationValue = computed(() => {
  if (!telemetryData.value?.jointSamples?.length) return null
  return telemetryData.value.jointSamples[0].vibrationRms
})
const temperatureValue = computed(() => {
  if (!telemetryData.value?.jointSamples?.length) return null
  return telemetryData.value.jointSamples[0].temperatureC
})

// 初始化
onMounted(async () => {
  await loadData()
  initCharts()
  robotStore.initWebSocketSubscription(robotId)
})

// 清理
onUnmounted(() => {
  robotStore.unsubscribeWebSocket(robotId)
  disposeCharts()
})

// 监听遥测数据变化
watch(() => robotStore.telemetryData, (newData) => {
  if (newData) {
    telemetryData.value = newData
    updateCharts()
  }
}, { deep: true })

// 加载数据
const loadData = async () => {
  loading.value = true
  error.value = ''

  try {
    await robotStore.loadRobot(robotId)
    await robotStore.loadTelemetry(robotId)
    await updateCharts()
  } catch (err: any) {
    error.value = err.message || '加载数据失败'
    ElMessage.error(error.value)
  } finally {
    loading.value = false
  }
}

// 初始化图表
const initCharts = () => {
  if (currentChartRef.value) {
    currentChart = echarts.init(currentChartRef.value)
  }
  if (vibrationChartRef.value) {
    vibrationChart = echarts.init(vibrationChartRef.value)
  }
  if (temperatureChartRef.value) {
    temperatureChart = echarts.init(temperatureChartRef.value)
  }

  // 监听窗口大小变化
  window.addEventListener('resize', resizeCharts)
}

// 销毁图表
const disposeCharts = () => {
  if (currentChart) currentChart.dispose()
  if (vibrationChart) vibrationChart.dispose()
  if (temperatureChart) temperatureChart.dispose()
  window.removeEventListener('resize', resizeCharts)
}

// 调整图表大小
const resizeCharts = () => {
  if (currentChart) currentChart.resize()
  if (vibrationChart) vibrationChart.resize()
  if (temperatureChart) temperatureChart.resize()
}

// 更新图表数据
const updateCharts = async () => {
  if (!robot.value) return

  const minutes = timeRange.value === '5m' ? 5 : timeRange.value === '30m' ? 30 : 15
  const from = new Date(Date.now() - minutes * 60 * 1000)

  try {
    // 加载时序数据
    const [currentData, vibrationData, temperatureData] = await Promise.all([
      robotStore.loadTelemetrySeries(robotId, 'current_a', from),
      robotStore.loadTelemetrySeries(robotId, 'vibration_rms', from),
      robotStore.loadTelemetrySeries(robotId, 'temperature_c', from),
    ])

    // 更新图表
    updateCurrentChart(currentData)
    updateVibrationChart(vibrationData)
    updateTemperatureChart(temperatureData)
  } catch (error) {
    console.error('Failed to update charts:', error)
  }
}

// 更新电流图表
const updateCurrentChart = (data: any[]) => {
  if (!currentChart) return

  const option = {
    title: { text: '关节电流 (A)' },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'time',
      name: '时间'
    },
    yAxis: {
      type: 'value',
      name: '电流 (A)'
    },
    series: [{
      name: '电流',
      type: 'line',
      smooth: true,
      data: data.map(item => [new Date(item.ts), item.currentA]),
      lineStyle: { color: '#409EFF' },
      itemStyle: { color: '#409EFF' }
    }],
    dataZoom: [{ type: 'inside' }, { type: 'slider' }]
  }

  currentChart.setOption(option, true)
}

// 更新振动图表
const updateVibrationChart = (data: any[]) => {
  if (!vibrationChart) return

  const option = {
    title: { text: '关节振动 (RMS)' },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'time',
      name: '时间'
    },
    yAxis: {
      type: 'value',
      name: '振动 (RMS)'
    },
    series: [{
      name: '振动',
      type: 'line',
      smooth: true,
      data: data.map(item => [new Date(item.ts), item.vibrationRms]),
      lineStyle: { color: '#E6A23C' },
      itemStyle: { color: '#E6A23C' }
    }],
    dataZoom: [{ type: 'inside' }, { type: 'slider' }]
  }

  vibrationChart.setOption(option, true)
}

// 更新温度图表
const updateTemperatureChart = (data: any[]) => {
  if (!temperatureChart) return

  const option = {
    title: { text: '关节温度 (°C)' },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'time',
      name: '时间'
    },
    yAxis: {
      type: 'value',
      name: '温度 (°C)'
    },
    series: [{
      name: '温度',
      type: 'line',
      smooth: true,
      data: data.map(item => [new Date(item.ts), item.temperatureC]),
      lineStyle: { color: '#F56C6C' },
      itemStyle: { color: '#F56C6C' }
    }],
    dataZoom: [{ type: 'inside' }, { type: 'slider' }]
  }

  temperatureChart.setOption(option, true)
}
</script>

<style scoped>
.robot-detail-page {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.robot-info h1 {
  margin: 0 0 8px 0;
  color: #303133;
}

.robot-meta {
  display: flex;
  gap: 12px;
  align-items: center;
}

.robot-meta span {
  color: #909399;
  font-size: 14px;
}

.status-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.status-card {
  text-align: center;
  padding: 16px;
}

.status-header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 12px;
  color: #606266;
}

.status-value {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 4px;
}

.status-value .value {
  font-size: 32px;
  font-weight: bold;
  color: #303133;
}

.status-value .unit {
  font-size: 16px;
  color: #909399;
}

.charts-section {
  display: grid;
  gap: 24px;
}

.chart-card {
  height: 400px;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-container {
  width: 100%;
  height: 320px;
}

.loading-state, .error-state {
  text-align: center;
  padding: 60px 20px;
}

.loading-state {
  color: #909399;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .status-cards {
    grid-template-columns: 1fr;
  }

  .charts-section {
    gap: 16px;
  }

  .chart-card {
    height: 300px;
  }

  .chart-container {
    height: 220px;
  }
}
</style>
