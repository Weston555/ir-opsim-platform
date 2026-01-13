import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Robot, TelemetryData } from '@/types/robot'
import { robotApi } from '@/api/robot'
import webSocketService from '@/api/websocket'

export const useRobotStore = defineStore('robot', () => {
  // 状态
  const robots = ref<Robot[]>([])
  const currentRobot = ref<Robot | null>(null)
  const telemetryData = ref<TelemetryData | null>(null)
  const loading = ref(false)

  // 计算属性
  const robotCount = computed(() => robots.value.length)
  const hasRobots = computed(() => robots.value.length > 0)

  // 初始化WebSocket订阅
  function initWebSocketSubscription(robotId: string) {
    // 订阅机器人最新数据推送
    webSocketService.subscribe(`/topic/robots/${robotId}/latest`, (data: TelemetryData) => {
      console.log('Received robot telemetry update:', data)
      telemetryData.value = data
    })
  }

  // 取消WebSocket订阅
  function unsubscribeWebSocket(robotId: string) {
    webSocketService.unsubscribe(`/topic/robots/${robotId}/latest`)
  }

  // 加载机器人列表
  async function loadRobots() {
    loading.value = true
    try {
      const data = await robotApi.getRobots()
      robots.value = data
    } catch (error) {
      console.error('Failed to load robots:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  // 加载机器人详情
  async function loadRobot(id: string) {
    const robot = robots.value.find(r => r.id === id)
    if (robot) {
      currentRobot.value = robot
      return robot
    }

    // 如果本地没有找到，从API获取
    try {
      await loadRobots()
      const found = robots.value.find(r => r.id === id)
      if (found) {
        currentRobot.value = found
        return found
      }
      throw new Error('Robot not found')
    } catch (error) {
      console.error('Failed to load robot:', error)
      throw error
    }
  }

  // 加载机器人遥测数据
  async function loadTelemetry(robotId: string) {
    try {
      const data = await robotApi.getLatestTelemetry(robotId)
      telemetryData.value = data
      return data
    } catch (error) {
      console.error('Failed to load telemetry:', error)
      throw error
    }
  }

  // 加载时序数据
  async function loadTelemetrySeries(robotId: string, metric: string, from?: Date, to?: Date, step = 100) {
    try {
      return await robotApi.getTelemetrySeries(robotId, { metric, from, to, step })
    } catch (error) {
      console.error('Failed to load telemetry series:', error)
      throw error
    }
  }

  // 设置当前机器人
  function setCurrentRobot(robot: Robot | null) {
    currentRobot.value = robot
  }

  return {
    // 状态
    robots,
    currentRobot,
    telemetryData,
    loading,

    // 计算属性
    robotCount,
    hasRobots,

    // 方法
    initWebSocketSubscription,
    unsubscribeWebSocket,
    loadRobots,
    loadRobot,
    loadTelemetry,
    loadTelemetrySeries,
    setCurrentRobot,
  }
})
