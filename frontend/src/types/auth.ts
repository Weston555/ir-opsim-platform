// 登录请求
export interface LoginRequest {
  username: string
  password: string
}

// 登录响应
export interface LoginResponse {
  token: string
  type: string
  id: string
  username: string
  email: string
  roles: string[]
}

// 用户信息
export interface UserInfo {
  id: string
  username: string
  email: string
  roles: string[]
}
