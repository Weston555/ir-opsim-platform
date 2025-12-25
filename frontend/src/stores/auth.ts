import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UserInfo, LoginRequest, LoginResponse } from '@/types/auth'
import { authApi } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  // 状态
  const token = ref<string | null>(localStorage.getItem('token'))
  const user = ref<UserInfo | null>(null)
  const loading = ref(false)

  // 计算属性
  const isAuthenticated = computed(() => !!token.value)
  const userRoles = computed(() => user.value?.roles || [])
  const isAdmin = computed(() => userRoles.value.includes('ROLE_ADMIN'))
  const isOperator = computed(() => userRoles.value.includes('ROLE_ADMIN') || userRoles.value.includes('ROLE_OPERATOR'))

  // 初始化用户信息
  if (token.value) {
    loadUserInfo()
  }

  // 登录
  async function login(loginRequest: LoginRequest): Promise<void> {
    loading.value = true
    try {
      const response: LoginResponse = await authApi.login(loginRequest)

      // 保存token和用户信息
      token.value = response.token
      user.value = {
        id: response.id,
        username: response.username,
        email: response.email,
        roles: response.roles,
      }

      // 保存到本地存储
      localStorage.setItem('token', response.token)
      localStorage.setItem('user', JSON.stringify(user.value))
    } catch (error) {
      throw error
    } finally {
      loading.value = false
    }
  }

  // 加载用户信息
  async function loadUserInfo(): Promise<void> {
    if (!token.value) return

    try {
      const userInfo = await authApi.getCurrentUser()
      user.value = userInfo
      localStorage.setItem('user', JSON.stringify(userInfo))
    } catch (error) {
      // 如果获取用户信息失败，清除token
      logout()
      throw error
    }
  }

  // 登出
  function logout(): void {
    token.value = null
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  // 检查权限
  function hasRole(role: string): boolean {
    return userRoles.value.includes(`ROLE_${role}`)
  }

  // 检查多个角色中的任意一个
  function hasAnyRole(roles: string[]): boolean {
    return roles.some(role => hasRole(role))
  }

  return {
    // 状态
    token,
    user,
    loading,

    // 计算属性
    isAuthenticated,
    userRoles,
    isAdmin,
    isOperator,

    // 方法
    login,
    loadUserInfo,
    logout,
    hasRole,
    hasAnyRole,
  }
})
