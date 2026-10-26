// 登录状态管理。
import { avatarUrl as resourceAvatarUrl } from '@/utils/resource'
import { defineStore } from 'pinia'
import type { UserVO } from '@/api/user.ts'
import { currentSessionApi, logoutApi, type LoginVO } from '@/api/auth.ts'
import { revokeCurrentSession } from '@/utils/auth-session'
import { Role } from '@/constants/role'
import {
  authorizationKey,
  normalizeRoles,
  persistAuth,
  readAuthCache,
  singleFlight,
  stringList,
  tokenPayload,
} from '@/utils/session-cache'

const loadSession = singleFlight(async (token: string) => (await currentSessionApi(token)).data)

export const useAuthStore = defineStore('auth', {
  state: () => ({
    loggingOut: false,
    verifiedToken: '',
    viewRevision: 0,
    ...readAuthCache(localStorage),
  }),

  actions: {
    setAuth(data: LoginVO) {
      this.verifiedToken = ''
      this.token = data.token
      this.user = data.user
      this.roles = normalizeRoles(data.roles)
      this.permissions = stringList(data.permissions)
      persistAuth(localStorage, this)
    },

    getToken() {
      return this.token
    },

    async refreshSession(): Promise<boolean> {
      const token = this.token
      const data = await loadSession(token)
      if (this.token !== token) return false
      if (
        !data?.user ||
        String(data.user.id) !== tokenPayload(token)?.sub ||
        !Array.isArray(data.roles) ||
        !Array.isArray(data.permissions)
      ) {
        throw new Error('Invalid session response')
      }
      const roles = normalizeRoles(data.roles)
      const permissions = stringList(data.permissions)
      if (
        this.verifiedToken !== token ||
        authorizationKey(this.roles, this.permissions) !== authorizationKey(roles, permissions)
      )
        this.viewRevision++
      this.user = data.user
      this.roles = roles
      this.permissions = permissions
      this.verifiedToken = token
      persistAuth(localStorage, this)
      return true
    },

    syncFromStorage() {
      this.verifiedToken = ''
      Object.assign(this, readAuthCache(localStorage))
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

    hasAnyRole(roles: readonly string[]) {
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
        // 服务端尚未确认会话撤销时保留当前会话，以便重试。
        // 失败提示由请求拦截器统一显示。
        return false
      } finally {
        this.loggingOut = false
      }
    },

    clearAuth() {
      this.verifiedToken = ''
      this.token = ''
      this.user = null
      this.roles = []
      this.permissions = []
      persistAuth(localStorage, this)
    },

    setUser(user: UserVO) {
      this.user = user
      persistAuth(localStorage, this)
    },

    updateAvatar(avatar: string) {
      if (this.user) {
        this.user.avatar = avatar
        persistAuth(localStorage, this)
      }
    },

    getAvatar() {
      const avatar = this.user?.avatar
      if (!avatar) {
        return ''
      }
      return resourceAvatarUrl(avatar)
    },
  },

  getters: {
    isSelf: (state) => (targetId: number) => {
      return state.user?.id === targetId
    },
  },
})
