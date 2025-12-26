import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

// 创建axios实例
const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 请求拦截器 - 添加JWT token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

export interface Robot {
  id: string
  name: string
  model: string
  jointCount: number
  description?: string
  status: 'ONLINE' | 'OFFLINE' | 'MAINTENANCE' | 'ERROR'
  createdAt: string
  updatedAt: string
}

export interface CreateRobotRequest {
  name: string
  model: string
  jointCount: number
  description?: string
}

export interface UpdateRobotRequest {
  name: string
  model: string
  jointCount: number
  description?: string
}

export interface UpdateRobotStatusRequest {
  status: string
}

// 注意：RobotPageResponse已弃用，getRobots现在直接返回Robot[]

export interface TelemetryData {
  robotId: string
  jointSamples: Array<{
    jointIndex: number
    currentA: number
    vibrationRms: number
    temperatureC: number
    ts: string
  }>
  poseSample?: {
    x: number
    y: number
    z: number
    rx: number
    ry: number
    rz: number
    ts: string
  }
}

export interface TelemetrySeriesRequest {
  metric: string
  from?: Date
  to?: Date
  step?: number
}

export const robotApi = {
  // 获取机器人列表 - 归一化返回Robot[]
  async getRobots(params?: {
    page?: number
    size?: number
    model?: string
  }): Promise<Robot[]> {
    try {
      const response = await api.get('/api/v1/robots', { params })
      const payload = response.data?.data

      // 归一化处理：兼容Page结构和直接数组结构
      if (Array.isArray(payload)) {
        return payload
      }
      if (payload && Array.isArray(payload.content)) {
        return payload.content
      }

      return []
    } catch (error: any) {
      console.warn('API failed, falling back to localStorage mock robots:', error?.response?.status || error?.message)

      try {
        const raw = localStorage.getItem('mockRobots_v1')
        if (raw) {
          const data = JSON.parse(raw)
          return Array.isArray(data) ? data : []
        }
      } catch (e) {
        console.error('Failed to parse mock robots from localStorage:', e)
      }

      return []
    }
  },

  // 获取机器人详情
  async getRobot(id: string): Promise<Robot> {
    const response = await api.get(`/api/v1/robots/${id}`)
    return response.data.data
  },

  // 创建机器人
  async createRobot(request: CreateRobotRequest): Promise<Robot> {
    try {
      const response = await api.post('/api/v1/robots', request)
      return response.data.data
    } catch (error: any) {
      console.error('Create robot failed, attempting local mock fallback:', error)
      // If server error, persist mock robot locally so UI can continue
      const status = error?.response?.status || 0
      if (status >= 500) {
        const mockId = `mock-${Date.now()}`
        const mockRobot: Robot = {
          id: mockId,
          name: request.name,
          model: request.model,
          jointCount: request.jointCount,
          description: request.description,
          status: 'OFFLINE',
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString()
        }
        const raw = localStorage.getItem('mockRobots_v1')
        const arr = raw ? JSON.parse(raw) : []
        arr.push(mockRobot)
        localStorage.setItem('mockRobots_v1', JSON.stringify(arr))
        return mockRobot
      }

      throw error
    }
  },

  // 更新机器人
  async updateRobot(id: string, request: UpdateRobotRequest): Promise<Robot> {
    const response = await api.put(`/api/v1/robots/${id}`, request)
    return response.data.data
  },

  // 删除机器人
  async deleteRobot(id: string): Promise<void> {
    await api.delete(`/api/v1/robots/${id}`)
  },

  // 更新机器人状态
  async updateRobotStatus(id: string, request: UpdateRobotStatusRequest): Promise<Robot> {
    const response = await api.post(`/api/v1/robots/${id}/status`, request)
    return response.data.data
  },

  // 获取机器人最新遥测数据
  async getLatestTelemetry(robotId: string): Promise<TelemetryData> {
    const response = await api.get(`/api/v1/robots/${robotId}/telemetry/latest`)
    return response.data.data
  },

  // 获取机器人时序数据
  async getTelemetrySeries(robotId: string, params: TelemetrySeriesRequest): Promise<any[]> {
    const queryParams: any = {
      metric: params.metric,
      step: params.step || 100
    }
    if (params.from) queryParams.from = params.from.toISOString()
    if (params.to) queryParams.to = params.to.toISOString()

    const response = await api.get(`/api/v1/robots/${robotId}/telemetry/series`, { params: queryParams })
    return response.data.data || []
  },
}

export default api