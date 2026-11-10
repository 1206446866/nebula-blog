// 登录状态管理。
import { avatarUrl as resourceAvatarUrl } from '@/utils/resource'
import { defineStore } from 'pinia'
import type { UserVO } from '@/api/user.ts'
import { currentSessionApi, logoutApi, type LoginVO } from '@/api/auth.ts'
import { revokeCurrentSession } from '@/utils/auth-session'
import { Role } from '@/constants/role'
import { ElMessageBox } from 'element-plus'
import { clearWritingCache, hasWritingCache } from '@/utils/private-cache'
import { flushSessionWriters } from '@/utils/session-exit'
import {
  authorizationKey,
  normalizeRoles,
  persistAuth,
  readAuthCache,
  singleFlight,
  stringList,
  tokenPayload,
  tokenAccountId,
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
      const previousId = this.user?.id ?? tokenAccountId(this.token)
      const changed = !!this.token && this.token !== data.token
      this.verifiedToken = ''
      this.token = data.token
      this.user = data.user
      this.roles = normalizeRoles(data.roles)
      this.permissions = stringList(data.permissions)
      if (changed) {
        // 先使旧标签页停写，再清旧账号备份，最后发布完整的新会话。
        persistAuth(localStorage, { ...this, token: '' })
        clearWritingCache(localStorage, previousId)
      }
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
      const previousId = this.user?.id ?? tokenAccountId(this.token)
      this.verifiedToken = ''
      // 跨标签页通知只读取快照，不清理正在由另一个页面发布的新会话。
      Object.assign(this, readAuthCache(localStorage, false))
      // 同账号的新会话可能已经开始写入，迟到的通知不能清除它的新备份。
      if (this.token && previousId !== this.user?.id) clearWritingCache(localStorage, previousId)
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
        const token = this.getToken()
        await flushSessionWriters(token)
        if (this.getToken() !== token) return false
        if (hasWritingCache(localStorage, this.user?.id)) {
          const accepted = await ElMessageBox.confirm(
            '本机仍有编辑备份，退出后会清除，包括其他标签页的备份。请先确认重要内容已手动保存、同步云端或导出；未同步内容清除后无法恢复。',
            '确认退出并清理本机备份？',
            {
              confirmButtonText: '清理并退出',
              cancelButtonText: '返回保存或导出',
              type: 'warning',
            },
          ).then(
            () => true,
            () => false,
          )
          if (!accepted || this.getToken() !== token) return false
        }
        return await revokeCurrentSession({
          token,
          revoke: logoutApi,
          currentToken: () => this.getToken(),
          clear: () => this.clearAuth(true),
        })
      } catch {
        // 服务端尚未确认会话撤销时保留当前会话，以便重试。
        // 失败提示由请求拦截器统一显示。
        return false
      } finally {
        this.loggingOut = false
      }
    },

    clearAuth(clearPrivate = false) {
      const previousId = this.user?.id ?? tokenAccountId(this.token)
      this.verifiedToken = ''
      this.token = ''
      this.user = null
      this.roles = []
      this.permissions = []
      persistAuth(localStorage, this)
      if (clearPrivate) clearWritingCache(localStorage, previousId)
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
