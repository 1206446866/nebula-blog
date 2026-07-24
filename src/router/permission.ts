// src/router/permission.ts
import router from './index'
import { useAuthStore } from '../stores/auth'

// 白名单页面无需登录
const whiteList = ['/login']

router.beforeEach((to) => {
  const userStore = useAuthStore()
  const token = userStore.getToken()

  // 白名单页面直接放行
  if (whiteList.includes(to.path)) return true

  // 未登录跳转 login
  if (!token && to.path !== '/login') {
    return {
      path: '/login',
      replace: true,
    }
  }

  //角色验证
  if (
    to.matched.some((record) => {
      const roles = record.meta.roles as string[] | undefined
      return roles && !userStore.hasAnyRole(roles)
    })
  ) {
    return '/403'
  }
  // 权限验证
  if (
    to.matched.some((record) => {
      const permission = record.meta.permission
      return permission && !userStore.hasPermission(permission as string)
    })
  ) {
    return '/403'
  }

  return true
})
