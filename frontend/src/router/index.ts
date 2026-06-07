import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '登录 - AI 口语陪练', guest: true }
  },
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/HomeView.vue'),
    meta: { title: 'AI 口语陪练' }
  },
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('@/views/SettingsView.vue'),
    meta: { title: '设置' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

function getToken(): string | null {
  return localStorage.getItem('token')
}

router.beforeEach((to, _from, next) => {
  document.title = (to.meta.title as string) || 'AI 英语口语陪练'

  const token = getToken()

  // 已登录用户访问登录页 → 重定向到首页
  if (to.meta.guest && token) {
    next('/')
    return
  }

  // 未登录用户访问非 guest 页面 → 重定向到登录页
  if (!to.meta.guest && !token) {
    next('/login')
    return
  }

  next()
})

export default router
