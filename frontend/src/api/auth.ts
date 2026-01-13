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
  // 登录 - 模拟登录，无论输入什么都成功
  async login(loginRequest: LoginRequest): Promise<LoginResponse> {
    // 模拟API延迟
    await new Promise(resolve => setTimeout(resolve, 500))

    // 生成模拟的JWT token (这是一个示例token，实际项目中应该从后端获取)
    const mockToken = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c'

    // 返回模拟的登录响应
    return {
      token: mockToken,
      type: 'Bearer',
      id: '1',
      username: loginRequest.username || 'admin',
      email: `${loginRequest.username || 'admin'}@iropsim.com`,
      roles: ['ROLE_ADMIN', 'ROLE_OPERATOR']
    }
  },

  // 获取当前用户信息 - 模拟数据
  async getCurrentUser(): Promise<UserInfo> {
    // 从localStorage获取已保存的用户信息
    const savedUser = localStorage.getItem('user')
    if (savedUser) {
      return JSON.parse(savedUser)
    }

    // 如果没有保存的用户信息，返回默认管理员用户
    return {
      id: '1',
      username: 'admin',
      email: 'admin@iropsim.com',
      roles: ['ROLE_ADMIN', 'ROLE_OPERATOR']
    }
  },

  // 登出
  logout() {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    window.location.href = '/login'
  },
}

export default api
