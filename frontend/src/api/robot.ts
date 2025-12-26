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

// 本地补全层：存储后端Robot实体缺少但UI需要的字段
export interface RobotMeta {
  status: 'ONLINE' | 'OFFLINE' | 'MAINTENANCE' | 'ERROR'
  description?: string
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

// RobotMeta 本地补全层工具函数
const ROBOT_META_KEY = 'robotMeta_v1'
const MOCK_ROBOTS_KEY = 'mockRobots_v1'

// 读取RobotMeta映射
function readMetaMap(): Record<string, RobotMeta> {
  try {
    const raw = localStorage.getItem(ROBOT_META_KEY)
    return raw ? JSON.parse(raw) : {}
  } catch (e) {
    console.error('Failed to read robot meta:', e)
    return {}
  }
}

// 写入RobotMeta映射
function writeMetaMap(metaMap: Record<string, RobotMeta>) {
  try {
    localStorage.setItem(ROBOT_META_KEY, JSON.stringify(metaMap))
    // 广播全局事件，让所有页面知道robots数据已更新
    window.dispatchEvent(new Event('robots-updated'))
  } catch (e) {
    console.error('Failed to write robot meta:', e)
  }
}

// 合并后端Robot与本地Meta
function mergeRobotWithMeta(backendRobot: Partial<Robot>): Robot {
  const metaMap = readMetaMap()
  const meta = metaMap[backendRobot.id || ''] || {
    status: 'OFFLINE' as const,
    description: '',
    updatedAt: backendRobot.createdAt || new Date().toISOString()
  }

  return {
    id: backendRobot.id || '',
    name: backendRobot.name || 'Unknown Robot',
    model: backendRobot.model || 'Unknown Model',
    jointCount: backendRobot.jointCount || 6,
    description: meta.description || backendRobot.description || '',
    status: meta.status,
    createdAt: backendRobot.createdAt || new Date().toISOString(),
    updatedAt: meta.updatedAt
  }
}

// 更新单个Robot的Meta
function updateRobotMeta(robotId: string, updates: Partial<RobotMeta>) {
  const metaMap = readMetaMap()
  const existingMeta = metaMap[robotId] || {
    status: 'OFFLINE' as const,
    description: '',
    updatedAt: new Date().toISOString()
  }

  metaMap[robotId] = { ...existingMeta, ...updates, updatedAt: new Date().toISOString() }
  writeMetaMap(metaMap)
}

// 读取mock robots
function readMockRobots(): Robot[] {
  try {
    const raw = localStorage.getItem(MOCK_ROBOTS_KEY)
    const robots = raw ? JSON.parse(raw) : []
    return robots.map(mergeRobotWithMeta)
  } catch (e) {
    console.error('Failed to read mock robots:', e)
    return []
  }
}

// 写入mock robots
function writeMockRobots(robots: Robot[]) {
  try {
    // 剥离meta字段，只保存基本字段
    const basicRobots = robots.map(robot => ({
      id: robot.id,
      name: robot.name,
      model: robot.model,
      jointCount: robot.jointCount,
      createdAt: robot.createdAt
    }))
    localStorage.setItem(MOCK_ROBOTS_KEY, JSON.stringify(basicRobots))
    window.dispatchEvent(new Event('robots-updated'))
  } catch (e) {
    console.error('Failed to write mock robots:', e)
  }
}

export const robotApi = {
  // 获取机器人列表 - 归一化返回Robot[]，自动merge本地meta
  async getRobots(params?: {
    page?: number
    size?: number
    model?: string
  }): Promise<Robot[]> {
    try {
      const response = await api.get('/api/v1/robots', { params })
      const payload = response.data?.data

      // 归一化处理：兼容Page结构和直接数组结构
      let backendRobots: Partial<Robot>[] = []
      if (Array.isArray(payload)) {
        backendRobots = payload
      } else if (payload && Array.isArray(payload.content)) {
        backendRobots = payload.content
      }

      // 逐个merge本地meta，返回完整的Robot对象
      return backendRobots.map(mergeRobotWithMeta)
    } catch (error: any) {
      // 后端失败时回退到localStorage的mock数据（已merge meta）
      console.warn('API failed, falling back to localStorage mock robots:', error?.response?.status || error?.message)
      return readMockRobots()
    }
  },

  // 获取机器人详情 - 自动merge本地meta
  async getRobot(id: string): Promise<Robot> {
    try {
      const response = await api.get(`/api/v1/robots/${id}`)
      return mergeRobotWithMeta(response.data.data)
    } catch (error: any) {
      console.warn('API failed, falling back to localStorage for robot detail:', error?.response?.status || error?.message)
      // 从mock robots中查找
      const mockRobots = readMockRobots()
      const robot = mockRobots.find(r => r.id === id)
      if (robot) {
        return robot
      }
      throw new Error('Robot not found')
    }
  },

  // 创建机器人
  async createRobot(request: CreateRobotRequest): Promise<Robot> {
    try {
      const response = await api.post('/api/v1/robots', request)
      const backendRobot = response.data.data

      // 成功时写入meta（status和description）
      updateRobotMeta(backendRobot.id, {
        status: 'OFFLINE',
        description: request.description || '',
        updatedAt: new Date().toISOString()
      })

      return mergeRobotWithMeta(backendRobot)
    } catch (error: any) {
      console.error('Create robot failed, attempting local mock fallback:', error)
      const status = error?.response?.status || 0
      const isDemoMode = localStorage.getItem('demoMode') === '1' ||
                        import.meta.env.VITE_DEMO_MODE === 'true'

      // 在演示模式下，对401/403/500等错误都fallback到localStorage
      if (isDemoMode && (status === 0 || status === 401 || status === 403 || status >= 500) ||
          (!isDemoMode && status >= 500)) {
        const mockId = `mock-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
        const mockRobot: Robot = {
          id: mockId,
          name: request.name,
          model: request.model,
          jointCount: request.jointCount,
          description: request.description || '',
          status: 'OFFLINE',
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString()
        }

        // 保存到localStorage（会自动dispatch事件）
        writeMockRobots([mockRobot])
        return mockRobot
      }

      throw error
    }
  },

  // 更新机器人 - 自动写入meta并同步
  async updateRobot(id: string, request: UpdateRobotRequest): Promise<Robot> {
    try {
      const response = await api.put(`/api/v1/robots/${id}`, request)
      const backendRobot = response.data.data

      // 成功时写入meta（description和updatedAt）
      updateRobotMeta(id, {
        description: request.description || '',
        updatedAt: new Date().toISOString()
      })

      return mergeRobotWithMeta(backendRobot)
    } catch (error: any) {
      console.warn('Update robot failed, falling back to localStorage:', error?.response?.status || error?.message)

      // 从mock robots中找到并更新
      const mockRobots = readMockRobots()
      const robotIndex = mockRobots.findIndex(r => r.id === id)
      if (robotIndex >= 0) {
        const updatedRobot = {
          ...mockRobots[robotIndex],
          name: request.name,
          model: request.model,
          jointCount: request.jointCount,
          description: request.description || '',
          updatedAt: new Date().toISOString()
        }
        mockRobots[robotIndex] = updatedRobot

        // 更新meta
        updateRobotMeta(id, {
          description: request.description || '',
          updatedAt: new Date().toISOString()
        })

        // 保存到localStorage
        writeMockRobots(mockRobots)
        return updatedRobot
      }

      throw error
    }
  },

  // 删除机器人 - 清理meta并同步
  async deleteRobot(id: string): Promise<void> {
    try {
      await api.delete(`/api/v1/robots/${id}`)
      // 成功时清理meta
      const metaMap = readMetaMap()
      delete metaMap[id]
      writeMetaMap(metaMap)
    } catch (error: any) {
      console.warn('Delete robot failed, using localStorage fallback:', error?.response?.status || error?.message)

      // 从mock robots中删除
      const mockRobots = readMockRobots().filter(r => r.id !== id)
      writeMockRobots(mockRobots)

      // 清理meta
      const metaMap = readMetaMap()
      delete metaMap[id]
      writeMetaMap(metaMap)
    }
  },

  // 更新机器人状态 - 自动写入meta并同步
  async updateRobotStatus(id: string, request: UpdateRobotStatusRequest): Promise<Robot> {
    try {
      // 后端可能不持久化状态，但我们仍然写入meta保证UI一致性
      updateRobotMeta(id, {
        status: request.status,
        updatedAt: new Date().toISOString()
      })

      const response = await api.post(`/api/v1/robots/${id}/status`, request)
      return mergeRobotWithMeta(response.data.data)
    } catch (error: any) {
      console.warn('Update robot status failed, using localStorage fallback:', error?.response?.status || error?.message)

      // 即使后端失败，也要写入meta保证UI更新
      updateRobotMeta(id, {
        status: request.status,
        updatedAt: new Date().toISOString()
      })

      // 从mock robots中返回更新后的robot
      const mockRobots = readMockRobots()
      const robot = mockRobots.find(r => r.id === id)
      if (robot) {
        return { ...robot, status: request.status, updatedAt: new Date().toISOString() }
      }

      throw error
    }
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