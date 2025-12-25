import { createRouter, createWebHistory } from 'vue-router'
import { authGuard, loginGuard } from './guards'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/pages/LoginPage.vue'),
      meta: { requiresAuth: false },
      beforeEnter: loginGuard,
    },
    {
      path: '/',
      name: 'dashboard',
      component: () => import('@/pages/DashboardPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/robots/:id',
      name: 'robot-detail',
      component: () => import('@/pages/RobotDetailPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/alarms',
      name: 'alarm-center',
      component: () => import('@/pages/AlarmCenterPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/alarms/:id',
      name: 'alarm-detail',
      component: () => import('@/pages/AlarmDetailPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/knowledge',
      name: 'knowledge',
      component: () => import('@/pages/KnowledgePage.vue'),
      meta: { requiresAuth: true, roles: ['ADMIN'] },
    },
  ]
})

// 添加全局守卫
router.beforeEach(authGuard)

export default router
