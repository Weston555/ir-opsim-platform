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
        <el-switch
          v-model="useMockTelemetry"
          active-text="使用模拟数据"
          inactive-text="使用真实数据"
          size="small"
          style="margin-left: 12px;"
        />
      </div>
    </div>

    <!-- 模拟数据参数面板 -->
    <el-collapse v-if="useMockTelemetry" style="margin-bottom: 20px;">
      <el-collapse-item title="模拟数据参数设置（论文可复现实验）" name="mock-params">
        <div class="mock-params-grid">
          <div class="param-group">
            <h4>可复现性</h4>
            <el-input-number
              v-model="mockSeed"
              :min="1"
              :max="999999"
              :step="1"
              label="种子值"
              placeholder="固定种子保证可复现"
              size="small"
              style="width: 200px;"
              @change="updateMockParams"
            />
          </div>

          <div class="param-group">
            <h4>数据范围</h4>
            <div class="range-inputs">
              <span>电流: </span>
              <el-input-number
                v-model="mockRanges.current.min"
                :min="0"
                :max="50"
                :step="0.1"
                size="small"
                style="width: 80px;"
                @change="updateMockParams"
              />
              <span> - </span>
              <el-input-number
                v-model="mockRanges.current.max"
                :min="0"
                :max="50"
                :step="0.1"
                size="small"
                style="width: 80px;"
                @change="updateMockParams"
              />
              <span>A</span>
            </div>

            <div class="range-inputs">
              <span>振动: </span>
              <el-input-number
                v-model="mockRanges.vibration.min"
                :min="0"
                :max="10"
                :step="0.01"
                size="small"
                style="width: 80px;"
                @change="updateMockParams"
              />
              <span> - </span>
              <el-input-number
                v-model="mockRanges.vibration.max"
                :min="0"
                :max="10"
                :step="0.01"
                size="small"
                style="width: 80px;"
                @change="updateMockParams"
              />
              <span>RMS</span>
            </div>

            <div class="range-inputs">
              <span>温度: </span>
              <el-input-number
                v-model="mockRanges.temperature.min"
                :min="0"
                :max="100"
                :step="1"
                size="small"
                style="width: 80px;"
                @change="updateMockParams"
              />
              <span> - </span>
              <el-input-number
                v-model="mockRanges.temperature.max"
                :min="0"
                :max="100"
                :step="1"
                size="small"
                style="width: 80px;"
                @change="updateMockParams"
              />
              <span>°C</span>
            </div>
          </div>

          <div class="param-group">
            <h4>噪声与周期</h4>
            <el-input-number
              v-model="mockNoise"
              :min="0"
              :max="5"
              :step="0.01"
              label="噪声幅度"
              size="small"
              style="width: 120px;"
              @change="updateMockParams"
            />
            <el-input-number
              v-model="mockTrend"
              :min="-0.1"
              :max="0.1"
              :step="0.001"
              label="趋势系数"
              size="small"
              style="width: 120px; margin-left: 10px;"
              @change="updateMockParams"
            />
            <el-input-number
              v-model="mockPeriod"
              :min="60"
              :max="1800"
              :step="30"
              label="周期(秒)"
              size="small"
              style="width: 120px; margin-left: 10px;"
              @change="updateMockParams"
            />
          </div>

          <div class="param-group">
            <h4>控制</h4>
            <el-input-number
              v-model="mockIntervalMs"
              :min="100"
              :max="5000"
              :step="100"
              label="更新间隔(ms)"
              size="small"
              style="width: 120px;"
              @change="updateMockParams"
            />
            <el-input-number
              v-model="mockMaxPoints"
              :min="100"
              :max="2000"
              :step="50"
              label="最大点数"
              size="small"
              style="width: 120px; margin-left: 10px;"
              @change="updateMockParams"
            />
            <el-button
              type="primary"
              size="small"
              style="margin-left: 10px;"
              @click="restartMockGenerators"
            >
              重新启动
            </el-button>
          </div>
        </div>
      </el-collapse-item>
    </el-collapse>

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
      <!-- 故障时间轴 -->
      <el-card class="timeline-card" v-if="faultInjections.length > 0">
        <template #header>
          <div class="card-header">
            <span>故障注入时间轴</span>
            <el-tag type="warning">{{ faultInjections.length }} 个故障事件</el-tag>
          </div>
        </template>

        <div class="timeline-container">
          <div class="current-time-indicator" :style="{ left: getCurrentTimePosition() + '%' }">
            <div class="time-label">{{ formatTime(new Date().toISOString()) }}</div>
            <div class="time-pointer"></div>
          </div>

          <div v-for="fault in faultInjections" :key="fault.id" class="fault-timeline-item">
            <div class="fault-label">
              <el-tag
                :type="getFaultTypeColor(fault.faultType)"
                size="small"
              >
                {{ getFaultTypeLabel(fault.faultType) }}
              </el-tag>
            </div>
            <div
              class="fault-bar"
              :style="getFaultBarStyle(fault)"
              :title="`${fault.faultType} - ${formatTime(fault.startTs)} 至 ${formatTime(fault.endTs)}`"
            ></div>
          </div>
        </div>

        <div class="timeline-legend">
          <div class="legend-item">
            <div class="legend-color overheat"></div>
            <span>过热</span>
          </div>
          <div class="legend-item">
            <div class="legend-color vibration"></div>
            <span>高振动</span>
          </div>
          <div class="legend-item">
            <div class="legend-color current"></div>
            <span>电流异常</span>
          </div>
          <div class="legend-item">
            <div class="legend-color drift"></div>
            <span>传感器漂移</span>
          </div>
        </div>
      </el-card>

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
import { ref, reactive, onMounted, onUnmounted, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
// icons are registered globally in main.ts; no local imports needed
import * as echarts from 'echarts'
import { useRobotStore } from '@/stores/robot'
import type { TelemetryData } from '@/types/robot'
import type { Robot } from '@/types/robot'
import { createMockSeriesGenerator, type MockSeriesGenerator } from '@/mock/telemetry'
import { readMockFaultInjections } from '@/mock/faults'

const route = useRoute()
const robotStore = useRobotStore()

// 状态
const robotId = route.params.id as string
const loading = ref(false)
const error = ref('')
const timeRange = ref('15m')
const telemetryData = ref<TelemetryData | null>(null)
const faultInjections = ref<any[]>([])
// mock telemetry control
const useMockTelemetry = ref(false)
let mockCurrentGen: MockSeriesGenerator | null = null
let mockVibrationGen: MockSeriesGenerator | null = null
let mockTemperatureGen: MockSeriesGenerator | null = null
let mockInterval: number | null = null
let mockActivatedByError = false

// mock parameters for reproducibility
const mockSeed = ref(12345)
const mockRanges = reactive({
  current: { min: 0, max: 20 },
  vibration: { min: 0, max: 5 },
  temperature: { min: 20, max: 90 }
})
const mockNoise = ref(0.5)
const mockTrend = ref(0.001)
const mockPeriod = ref(300)
const mockIntervalMs = ref(1000)
const mockMaxPoints = ref(900)

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
  // prefer mock when enabled or when mock activated due to error
  if (useMockTelemetry.value || mockActivatedByError) {
    const s = mockCurrentGen?.getSeries() || []
    return s.length ? s[s.length - 1].value : null
  }
  if (!telemetryData.value?.jointSamples?.length) return null
  return telemetryData.value.jointSamples[0].currentA
})
const vibrationValue = computed(() => {
  if (useMockTelemetry.value || mockActivatedByError) {
    const s = mockVibrationGen?.getSeries() || []
    return s.length ? s[s.length - 1].value : null
  }
  if (!telemetryData.value?.jointSamples?.length) return null
  return telemetryData.value.jointSamples[0].vibrationRms
})
const temperatureValue = computed(() => {
  if (useMockTelemetry.value || mockActivatedByError) {
    const s = mockTemperatureGen?.getSeries() || []
    return s.length ? s[s.length - 1].value : null
  }
  if (!telemetryData.value?.jointSamples?.length) return null
  return telemetryData.value.jointSamples[0].temperatureC
})

// 初始化
onMounted(async () => {
  await loadData()
  initCharts()
  robotStore.initWebSocketSubscription(robotId)
  // init mock generators with sensible defaults
  mockCurrentGen = createMockSeriesGenerator({
    metric: 'current_a',
    startValue: 2.5,
    min: 0,
    max: 20,
    noise: 0.1,
    intervalMs: 1000,
    maxPoints: 900
  })
  mockVibrationGen = createMockSeriesGenerator({
    metric: 'vibration_rms',
    startValue: 0.1,
    min: 0,
    max: 5,
    noise: 0.01,
    intervalMs: 1000,
    maxPoints: 900
  })
  mockTemperatureGen = createMockSeriesGenerator({
    metric: 'temperature_c',
    startValue: 40,
    min: 20,
    max: 90,
    noise: 0.2,
    intervalMs: 1000,
    maxPoints: 900
  })
  // when switch toggles, start/stop mock stream
  watch(useMockTelemetry, (val) => {
    if (val) startMockStream()
    else stopMockStream()
  })
})

// 清理
onUnmounted(() => {
  robotStore.unsubscribeWebSocket(robotId)
  disposeCharts()
  stopMockStream()
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
    await loadFaultInjections()
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

  // If using mock telemetry explicitly, feed charts from mock generators
  if (useMockTelemetry.value || mockActivatedByError) {
    updateCurrentChart(mockCurrentGen?.getSeries() || [])
    updateVibrationChart(mockVibrationGen?.getSeries() || [])
    updateTemperatureChart(mockTemperatureGen?.getSeries() || [])
    return
  }

  try {
    // 加载时序数据
    const [currentData, vibrationData, temperatureData] = await Promise.all([
      robotStore.loadTelemetrySeries(robotId, 'current_a', from),
      robotStore.loadTelemetrySeries(robotId, 'vibration_rms', from),
      robotStore.loadTelemetrySeries(robotId, 'temperature_c', from),
    ])

    // 如果后端返回空或异常数据，降级到mock
    const valid = (arr: any[]) => Array.isArray(arr) && arr.length > 0
    if (!valid(currentData) && !valid(vibrationData) && !valid(temperatureData)) {
      // 自动降级并提示一次
      mockActivatedByError = true
      ElMessage.warning('遥测数据为空或不可用，已切换到模拟数据')
      startMockStream()
      updateCurrentChart(mockCurrentGen?.getSeries() || [])
      updateVibrationChart(mockVibrationGen?.getSeries() || [])
      updateTemperatureChart(mockTemperatureGen?.getSeries() || [])
      return
    }

    // 更新图表 (后端返回的series应为 {ts, value} 格式)
    updateCurrentChart(currentData)
    updateVibrationChart(vibrationData)
    updateTemperatureChart(temperatureData)
  } catch (error: any) {
    console.error('Failed to update charts:', error)
    // 降级到mock模式
    mockActivatedByError = true
    ElMessage.warning('获取遥测数据失败，已切换到模拟数据')
    startMockStream()
    updateCurrentChart(mockCurrentGen?.getSeries() || [])
    updateVibrationChart(mockVibrationGen?.getSeries() || [])
    updateTemperatureChart(mockTemperatureGen?.getSeries() || [])
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
      data: data.map((p: any) => [new Date(p.ts), p.value]),
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
      data: data.map((p: any) => [new Date(p.ts), p.value]),
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

  // 计算是否有超温数据（用于背景着色）
  const hasHighTemp = data.some((p: any) => p.value > 70)

  const option = {
    title: { text: '关节温度 (°C)' },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'time',
      name: '时间'
    },
    yAxis: {
      type: 'value',
      name: '温度 (°C)',
      // 添加预警区域背景
      splitArea: {
        show: true,
        areaStyle: {
          color: hasHighTemp ? ['rgba(245, 108, 108, 0.1)', 'transparent'] : 'transparent'
        }
      }
    },
    series: [
      {
        name: '温度',
        type: 'line',
        smooth: true,
        data: data.map((p: any) => [new Date(p.ts), p.value]),
        lineStyle: { color: '#F56C6C' },
        itemStyle: { color: '#F56C6C' },
        // 高亮超温点
        markPoint: {
          data: data.filter((p: any) => p.value > 70).map((p: any) => ({
            coord: [new Date(p.ts), p.value],
            itemStyle: { color: '#F56C6C' },
            symbolSize: 8
          }))
        }
      },
      // 预警阈值线
      {
        name: '预警线',
        type: 'line',
        data: [],
        markLine: {
          silent: true,
          lineStyle: {
            color: '#E6A23C',
            type: 'dashed'
          },
          data: [{
            yAxis: 70,
            label: {
              formatter: '70°C 预警线',
              position: 'start'
            }
          }]
        }
      }
    ],
    dataZoom: [{ type: 'inside' }, { type: 'slider' }]
  }

  temperatureChart.setOption(option, true)
}

// Mock stream control
const startMockStream = () => {
  if (mockInterval !== null) return
  mockCurrentGen?.start()
  mockVibrationGen?.start()
  mockTemperatureGen?.start()
  // update charts immediately and then per second
  updateCurrentChart(mockCurrentGen?.getSeries() || [])
  updateVibrationChart(mockVibrationGen?.getSeries() || [])
  updateTemperatureChart(mockTemperatureGen?.getSeries() || [])
  mockInterval = window.setInterval(() => {
    updateCurrentChart(mockCurrentGen?.getSeries() || [])
    updateVibrationChart(mockVibrationGen?.getSeries() || [])
    updateTemperatureChart(mockTemperatureGen?.getSeries() || [])
  }, 1000)
}

const stopMockStream = () => {
  if (mockInterval !== null) {
    clearInterval(mockInterval)
    mockInterval = null
  }
  mockCurrentGen?.stop()
  mockVibrationGen?.stop()
  mockTemperatureGen?.stop()
  mockActivatedByError = false
}

// 加载故障注入数据
const loadFaultInjections = async () => {
  const robotId = route.params.id as string

  // 先读取本地演示故障注入记录（按robotId过滤）
  const localInjections = readMockFaultInjections()
    .filter(f => !f.robotId || f.robotId === robotId)
    .slice(-200) // 限制最近200条

  // 设置本地记录（演示优先）
  faultInjections.value = localInjections.filter(fault =>
    fault.startTs && fault.endTs &&
    new Date(fault.endTs) > new Date(Date.now() - 24 * 60 * 60 * 1000) // 只显示24小时内的故障
  )

  // 尝试获取后端数据补充（失败不影响UI）
  try {
    const response = await fetch('/api/v1/sim/runs')
    const runs = response.ok ? (await response.json()).data || [] : []

    const allFaults: any[] = []
    for (const run of runs) {
      if (run.status === 'RUNNING' || run.status === 'STOPPED') {
        try {
          const faultResponse = await fetch(`/api/v1/sim/runs/${run.id}/faults`)
          if (faultResponse.ok) {
            const faults = (await faultResponse.json()).data || []
            allFaults.push(...faults)
          }
        } catch (e) {
          // 忽略单个运行的错误
        }
      }
    }

    const backendFaults = allFaults.filter(fault =>
      fault.startTs && fault.endTs &&
      new Date(fault.endTs) > new Date(Date.now() - 24 * 60 * 60 * 1000)
    )

    // 合并本地和后端数据（去重）
    const byId = new Map<string, any>()
    for (const f of [...localInjections, ...backendFaults]) {
      byId.set(`${f.scenarioRun?.id || 'unknown'}-${f.faultType}-${f.startTs}`, f)
    }
    faultInjections.value = Array.from(byId.values())
  } catch (error) {
    // 静默失败，本地数据已设置
    console.warn('Failed to load backend fault injections, using local data only:', error)
  }
}

// mock参数更新
const updateMockParams = () => {
  if (!useMockTelemetry.value && !mockActivatedByError) return

  // 更新所有生成器的参数
  if (mockCurrentGen) {
    mockCurrentGen.updateParams({
      min: mockRanges.current.min,
      max: mockRanges.current.max,
      noise: mockNoise.value,
      trend: mockTrend.value,
      intervalMs: mockIntervalMs.value,
      maxPoints: mockMaxPoints.value,
      period: mockPeriod.value
    })
  }

  if (mockVibrationGen) {
    mockVibrationGen.updateParams({
      min: mockRanges.vibration.min,
      max: mockRanges.vibration.max,
      noise: mockNoise.value,
      trend: mockTrend.value,
      intervalMs: mockIntervalMs.value,
      maxPoints: mockMaxPoints.value,
      period: mockPeriod.value
    })
  }

  if (mockTemperatureGen) {
    mockTemperatureGen.updateParams({
      min: mockRanges.temperature.min,
      max: mockRanges.temperature.max,
      noise: mockNoise.value,
      trend: mockTrend.value,
      intervalMs: mockIntervalMs.value,
      maxPoints: mockMaxPoints.value,
      period: mockPeriod.value
    })
  }
}

// 重新启动mock生成器
const restartMockGenerators = () => {
  // 停止现有生成器
  stopMockStream()

  // 设置种子
  mockCurrentGen?.setSeed(mockSeed.value)
  mockVibrationGen?.setSeed(mockSeed.value + 1)
  mockTemperatureGen?.setSeed(mockSeed.value + 2)

  // 启动生成器
  startMockStream()
}

// 时间轴相关方法
const getCurrentTimePosition = () => {
  if (faultInjections.value.length === 0) return 0

  const now = Date.now()
  const earliestFault = faultInjections.value.reduce((earliest, fault) =>
    Math.min(earliest, new Date(fault.startTs).getTime()), now)
  const latestFault = faultInjections.value.reduce((latest, fault) =>
    Math.max(latest, new Date(fault.endTs).getTime()), now)

  const startTime = Math.min(earliestFault, now - 60 * 60 * 1000) // 至少1小时前
  const endTime = Math.max(latestFault, now + 60 * 60 * 1000)   // 至少1小时后

  if (now < startTime) return 0
  if (now > endTime) return 100

  return ((now - startTime) / (endTime - startTime)) * 100
}

const getFaultBarStyle = (fault: any) => {
  if (faultInjections.value.length === 0) return {}

  const now = Date.now()
  const earliestFault = faultInjections.value.reduce((earliest, f) =>
    Math.min(earliest, new Date(f.startTs).getTime()), now)
  const latestFault = faultInjections.value.reduce((latest, f) =>
    Math.max(latest, new Date(f.endTs).getTime()), now)

  const startTime = Math.min(earliestFault, now - 60 * 60 * 1000)
  const endTime = Math.max(latestFault, now + 60 * 60 * 1000)

  const faultStart = new Date(fault.startTs).getTime()
  const faultEnd = new Date(fault.endTs).getTime()

  const startPercent = Math.max(0, ((faultStart - startTime) / (endTime - startTime)) * 100)
  const endPercent = Math.min(100, ((faultEnd - startTime) / (endTime - startTime)) * 100)
  const widthPercent = Math.max(1, endPercent - startPercent)

  return {
    left: startPercent + '%',
    width: widthPercent + '%',
    backgroundColor: getFaultColor(fault.faultType)
  }
}

const getFaultTypeColor = (faultType: string) => {
  switch (faultType) {
    case 'OVERHEAT': return 'danger'
    case 'HIGH_VIBRATION': return 'warning'
    case 'CURRENT_SPIKE': return 'danger'
    case 'SENSOR_DRIFT': return 'info'
    default: return 'info'
  }
}

const getFaultTypeLabel = (faultType: string) => {
  switch (faultType) {
    case 'OVERHEAT': return '过热'
    case 'HIGH_VIBRATION': return '高振动'
    case 'CURRENT_SPIKE': return '电流异常'
    case 'SENSOR_DRIFT': return '传感器漂移'
    default: return faultType
  }
}

const getFaultColor = (faultType: string) => {
  switch (faultType) {
    case 'OVERHEAT': return '#F56C6C'
    case 'HIGH_VIBRATION': return '#E6A23C'
    case 'CURRENT_SPIKE': return '#F56C6C'
    case 'SENSOR_DRIFT': return '#909399'
    default: return '#C0C4CC'
  }
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

.mock-params-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
  padding: 16px 0;
}

.param-group {
  background: #f8fafc;
  padding: 16px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.param-group h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

.range-inputs {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.range-inputs span {
  font-size: 12px;
  color: #6b7280;
  white-space: nowrap;
}

.charts-section {
  display: grid;
  gap: 24px;
}

.timeline-card {
  margin-bottom: 24px;
}

.timeline-container {
  position: relative;
  height: 200px;
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 20px;
  border: 1px solid #f0f2f5;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
}

.current-time-indicator {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 2px;
  background: #409EFF;
  z-index: 10;
}

.time-label {
  position: absolute;
  top: -25px;
  left: 50%;
  transform: translateX(-50%);
  background: #409EFF;
  color: white;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  white-space: nowrap;
}

.time-pointer {
  position: absolute;
  top: -5px;
  left: -4px;
  width: 0;
  height: 0;
  border-left: 4px solid transparent;
  border-right: 4px solid transparent;
  border-top: 5px solid #409EFF;
}

.fault-timeline-item {
  position: relative;
  height: 32px;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
}

.fault-label {
  width: 120px;
  flex-shrink: 0;
  text-align: right;
  margin-right: 12px;
  font-weight: 600;
  color: #374151;
  font-size: 13px;
}

.fault-bar {
  height: 20px;
  border-radius: 10px;
  position: relative;
  opacity: 0.9;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  cursor: pointer;
}

.fault-bar:hover {
  opacity: 1;
  transform: scaleY(1.2);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.timeline-legend {
  display: flex;
  gap: 16px;
  justify-content: center;
  flex-wrap: wrap;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}

.legend-color {
  width: 14px;
  height: 14px;
  border-radius: 2px;
}

.legend-color.overheat {
  background: #F56C6C;
}

.legend-color.vibration {
  background: #E6A23C;
}

.legend-color.current {
  background: #F56C6C;
}

.legend-color.drift {
  background: #909399;
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
