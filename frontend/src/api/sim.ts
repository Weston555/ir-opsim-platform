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

export interface ScenarioRunRequest {
  scenarioId: string
  mode: string
  seed?: number
  rateHz: number
}

export interface FaultInjectionRequest {
  faultType: string
  startTs: string
  endTs: string
  params: Record<string, any>
}

export interface ScenarioRun {
  id: string
  scenario: {
    id: string
    name: string
  }
  mode: string
  status: string
  seed: number
  rateHz: number
  createdAt: string
  startedAt?: string
  endedAt?: string
}

export interface FaultInjection {
  id: number
  scenarioRun: ScenarioRun
  faultType: string
  startTs: string
  endTs: string
  params: Record<string, any>
  createdAt: string
}

export const simApi = {
  // 获取所有仿真运行
  async getScenarioRuns(): Promise<ScenarioRun[]> {
    const response = await api.get('/api/v1/sim/runs')
    return response.data.data || []
  },

  // 获取指定仿真运行
  async getScenarioRun(id: string): Promise<ScenarioRun> {
    const response = await api.get(`/api/v1/sim/runs/${id}`)
    return response.data.data
  },

  // 创建仿真运行
  async createScenarioRun(request: ScenarioRunRequest): Promise<ScenarioRun> {
    const response = await api.post('/api/v1/sim/runs', request)
    return response.data.data
  },

  // 启动仿真运行
  async startScenarioRun(id: string): Promise<void> {
    await api.post(`/api/v1/sim/runs/${id}/start`)
  },

  // 停止仿真运行
  async stopScenarioRun(id: string): Promise<void> {
    await api.post(`/api/v1/sim/runs/${id}/stop`)
  },

  // 开始回放
  async startReplay(id: string, speed: number = 1.0): Promise<void> {
    await api.post(`/api/v1/sim/runs/${id}/replay`, null, {
      params: { speed }
    })
  },

  // 停止回放
  async stopReplay(id: string): Promise<void> {
    await api.post(`/api/v1/sim/runs/${id}/replay/stop`)
  },

  // 添加故障注入
  async addFaultInjection(runId: string, request: FaultInjectionRequest): Promise<FaultInjection> {
    const response = await api.post(`/api/v1/sim/runs/${runId}/faults`, request)
    return response.data.data
  },

  // 获取故障注入列表
  async getFaultInjections(runId: string): Promise<FaultInjection[]> {
    const response = await api.get(`/api/v1/sim/runs/${runId}/faults`)
    return response.data.data || []
  },

  // 获取故障模板列表
  async getFaultTemplates(params?: {
    faultType?: string
    severity?: string
  }): Promise<FaultTemplate[]> {
    const response = await api.get('/api/v1/sim/fault-templates', { params })
    return response.data.data || []
  },
}

export default api
