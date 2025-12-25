import api from '@/api/auth'
import type { Robot, TelemetryData } from '@/types/robot'

// 机器人相关API
export const robotApi = {
  // 获取机器人列表
  async getRobots(): Promise<Robot[]> {
    const response = await api.get('/api/v1/robots')
    return response.data.data
  },

  // 获取机器人最新遥测数据
  async getLatestTelemetry(robotId: string): Promise<TelemetryData> {
    const response = await api.get(`/api/v1/robots/${robotId}/latest`)
    return response.data.data
  },

  // 获取机器人时序数据
  async getTelemetrySeries(robotId: string, params: {
    metric: string
    from?: Date
    to?: Date
    step?: number
  }) {
    const queryParams = new URLSearchParams({
      metric: params.metric,
      step: (params.step || 100).toString(),
    })

    if (params.from) queryParams.append('from', params.from.toISOString())
    if (params.to) queryParams.append('to', params.to.toISOString())

    const response = await api.get(`/api/v1/robots/${robotId}/joints/0/series?${queryParams.toString()}`)
    return response.data.data
  },
}
