// src/stores/auth.ts
import { defineStore } from 'pinia'
import type { UserVO } from '@/api/user.ts'
import { logoutApi, type LoginVO } from '@/api/auth.ts'
import { revokeCurrentSession } from '@/utils/auth-session'
import { Role } from '@/constants/role'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    loggingOut: false,
    token: localStorage.getItem('token') || '', //防止 token 变成 null。
    user: localStorage.getItem('user') ? JSON.parse(localStorage.getItem('user')!) : null,
    roles: localStorage.getItem('roles') ? JSON.parse(localStorage.getItem('roles')!) : [],
    permissions: localStorage.getItem('permissions')
      ? JSON.parse(localStorage.getItem('permissions')!)
      : [],
  }),

  actions: {
    setAuth(data: LoginVO) {
      this.token = data.token
      this.user = data.user
      this.roles = data.roles || []
      this.permissions = data.permissions || []
      localStorage.setItem('token', data.token)
      localStorage.setItem('user', JSON.stringify(data.user))
      localStorage.setItem('roles', JSON.stringify(this.roles))
      localStorage.setItem('permissions', JSON.stringify(this.permissions))
    },

    getToken() {
      return this.token || localStorage.getItem('token') || ''
    },

    hasRole(role: string) {
      if (this.roles.includes(role)) return true
      if (
        this.roles.includes(Role.SUPER_ADMIN) &&
        [Role.ADMIN, Role.USER].some((item) => item === role)
      )
        return true
      return role === Role.USER && this.roles.includes(Role.ADMIN)
    },

    hasAnyRole(roles: string[]) {
      return roles.some((role) => this.hasRole(role))
    },

    hasPermission(permission: string) {
      return this.roles.includes(Role.SUPER_ADMIN) || this.permissions.includes(permission)
    },

    hasAnyPermission(permissions: string[]) {
      return permissions.some((permission) => this.hasPermission(permission))
    },

    async logout(): Promise<boolean> {
      if (this.loggingOut) return false
      this.loggingOut = true
      try {
        return await revokeCurrentSession({
          token: this.getToken(),
          revoke: logoutApi,
          currentToken: () => this.getToken(),
          clear: () => this.clearAuth(),
        })
      } catch {
        // Keep the session available for retry when the server cannot confirm revocation.
        // The request interceptor displays the failure.
        return false
      } finally {
        this.loggingOut = false
      }
    },

    clearAuth() {
      this.token = ''
      this.user = null
      this.roles = []
      this.permissions = []
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      localStorage.removeItem('roles')
      localStorage.removeItem('permissions')
    },

    setUser(user: UserVO) {
      this.user = user
      localStorage.setItem('user', JSON.stringify(user))
    },

    updateAvatar(avatar: string) {
      if (this.user) {
        this.user.avatar = avatar
        localStorage.setItem('user', JSON.stringify(this.user))
      }
    },

    getAvatar() {
      const avatar = this.user?.avatar
      if (!avatar) {
        return ''
      }
      return `http://localhost:8080/upload/avatar/${avatar}`
    },
  },

  getters: {
    isSelf: (state) => (targetId: number) => {
      return state.user?.id === targetId
    },
  },
})
