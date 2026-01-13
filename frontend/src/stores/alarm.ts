import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { AlarmEvent, AlarmQuery } from '@/types/alarm'
import { alarmApi } from '@/api/alarm'
import webSocketService from '@/api/websocket'

export const useAlarmStore = defineStore('alarm', () => {
  // 状态
  const alarms = ref<AlarmEvent[]>([])
  const loading = ref(false)
  const openAlarmCount = ref(0)

  // 计算属性
  const criticalAlarms = computed(() =>
    alarms.value.filter(alarm => alarm.severity === 'CRITICAL' && alarm.status === 'OPEN')
  )

  const warningAlarms = computed(() =>
    alarms.value.filter(alarm => alarm.severity === 'WARN' && alarm.status === 'OPEN')
  )

  const infoAlarms = computed(() =>
    alarms.value.filter(alarm => alarm.severity === 'INFO' && alarm.status === 'OPEN')
  )

  // 初始化WebSocket订阅
  function initWebSocketSubscription() {
    // 订阅告警推送
    webSocketService.subscribe('/topic/alarms', (alarm: AlarmEvent) => {
      console.log('Received alarm update:', alarm)

      // 更新告警列表
      loadAlarms()

      // 更新告警计数
      updateAlarmCount()
    })
  }

  // 加载告警列表
  async function loadAlarms(query: AlarmQuery = { page: 0, size: 50 }) {
    loading.value = true
    try {
      const result = await alarmApi.getAlarms(query)
      alarms.value = result.content || result // 兼容分页和非分页响应
      return result
    } catch (error) {
      console.error('Failed to load alarms:', error)
      return null
    } finally {
      loading.value = false
    }
  }

  // 获取告警详情
  async function getAlarm(id: string): Promise<AlarmEvent | null> {
    try {
      return await alarmApi.getAlarm(id)
    } catch (error) {
      console.error('Failed to get alarm:', error)
      return null
    }
  }

  // 确认告警
  async function acknowledgeAlarm(id: string, comment?: string) {
    try {
      await alarmApi.acknowledgeAlarm(id, comment)
      await loadAlarms() // 重新加载列表
    } catch (error) {
      console.error('Failed to acknowledge alarm:', error)
      throw error
    }
  }

  // 关闭告警
  async function closeAlarm(id: string) {
    try {
      await alarmApi.closeAlarm(id)
      await loadAlarms() // 重新加载列表
    } catch (error) {
      console.error('Failed to close alarm:', error)
      throw error
    }
  }

  // 获取告警建议
  async function getAlarmRecommendation(id: string) {
    try {
      return await alarmApi.getAlarmRecommendation(id)
    } catch (error) {
      console.error('Failed to get alarm recommendation:', error)
      throw error
    }
  }

  // 更新告警计数
  function updateAlarmCount() {
    openAlarmCount.value = alarms.value.filter(alarm => alarm.status === 'OPEN').length
  }

  return {
    // 状态
    alarms,
    loading,
    openAlarmCount,

    // 计算属性
    criticalAlarms,
    warningAlarms,
    infoAlarms,

    // 方法
    initWebSocketSubscription,
    loadAlarms,
    getAlarm,
    acknowledgeAlarm,
    closeAlarm,
    getAlarmRecommendation,
    updateAlarmCount,
  }
})
