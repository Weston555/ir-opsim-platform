import api from '@/api/auth'
import type { AlarmEvent, AlarmQuery, AlarmRecommendation } from '@/types/alarm'

// 告警相关API
export const alarmApi = {
  // 获取告警列表
  async getAlarms(query: AlarmQuery = {}) {
    const params = new URLSearchParams()

    if (query.page !== undefined) params.append('page', query.page.toString())
    if (query.size !== undefined) params.append('size', query.size.toString())
    if (query.status) params.append('status', query.status)
    if (query.severity) params.append('severity', query.severity)
    if (query.robotId) params.append('robotId', query.robotId)
    if (query.from) params.append('from', query.from.toISOString())
    if (query.to) params.append('to', query.to.toISOString())

    const response = await api.get(`/api/v1/alarms?${params.toString()}`)
    return response.data.data
  },

  // 获取告警详情
  async getAlarm(id: string): Promise<AlarmEvent> {
    const response = await api.get(`/api/v1/alarms/${id}`)
    return response.data.data
  },

  // 确认告警
  async acknowledgeAlarm(id: string, comment?: string) {
    const response = await api.post(`/api/v1/alarms/${id}/ack`, { comment })
    return response.data
  },

  // 关闭告警
  async closeAlarm(id: string) {
    const response = await api.post(`/api/v1/alarms/${id}/close`)
    return response.data
  },

  // 获取告警建议
  async getAlarmRecommendation(id: string): Promise<AlarmRecommendation> {
    const response = await api.get(`/api/v1/alarms/${id}/recommendation`)
    return response.data.data
  },
}
