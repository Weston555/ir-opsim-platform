import axios from 'axios'
import type { LoginRequest, LoginResponse, UserInfo } from '@/types/auth'

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

// 响应拦截器 - 处理401错误
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // 清除本地存储的token
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      // 重定向到登录页
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export const authApi = {
  // 登录
  async login(loginRequest: LoginRequest): Promise<LoginResponse> {
    const response = await api.post('/api/v1/auth/login', loginRequest)
    return response.data.data
  },

  // 获取当前用户信息
  async getCurrentUser(): Promise<UserInfo> {
    const response = await api.get('/api/v1/auth/me')
    return response.data.data
  },

  // 登出
  logout() {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    window.location.href = '/login'
  },
}

export default api
