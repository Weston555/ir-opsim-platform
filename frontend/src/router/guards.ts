import { useAuthStore } from '@/stores/auth'
import type { NavigationGuard } from 'vue-router'

// 认证守卫
export const authGuard: NavigationGuard = (to, from, next) => {
  const authStore = useAuthStore()

  // 检查是否需要认证
  if (to.meta.requiresAuth !== false) {
    if (!authStore.isAuthenticated) {
      // 未认证，重定向到登录页
      next({
        path: '/login',
        query: { redirect: to.fullPath }
      })
      return
    }
  }

  // 检查角色权限
  if (to.meta.roles && Array.isArray(to.meta.roles)) {
    const hasRole = authStore.hasAnyRole(to.meta.roles)
    if (!hasRole) {
      // 无权限，重定向到首页
      next('/')
      return
    }
  }

  next()
}

// 登录页守卫（已登录用户访问登录页时重定向）
export const loginGuard: NavigationGuard = (to, from, next) => {
  const authStore = useAuthStore()

  if (to.path === '/login' && authStore.isAuthenticated) {
    // 已登录，重定向到首页
    next('/')
    return
  }

  next()
}
