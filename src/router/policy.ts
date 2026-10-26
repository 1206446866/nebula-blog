import { tokenPayload } from '../utils/session-cache.ts'
import { positiveId, readArticleQuery } from '../utils/article-query.ts'

export function safeRedirect(value: unknown): string | null {
  if (typeof value !== 'string' || !value.startsWith('/') || /[\\\u0000-\u0020]/.test(value))
    return null
  try {
    const decoded = decodeURIComponent(value)
    if (decoded.startsWith('//') || /[\\\u0000-\u001f]/.test(decoded)) return null
    const url = new URL(value, 'https://nebula.local')
    if (
      url.origin !== 'https://nebula.local' ||
      ['/login', '/register', '/session-error', '/403', '/404'].includes(
        decodeURIComponent(url.pathname).replace(/\/+$/, '').toLowerCase(),
      )
    )
      return null
    return url.pathname + url.search + url.hash
  } catch {
    return null
  }
}

type Target = {
  fullPath: string
  meta: { public?: boolean; idParam?: string; articleQuery?: boolean }
  params: Record<string, unknown>
  query?: Record<string, unknown>
  matched: { meta: { roles?: readonly string[]; permission?: string } }[]
}
type Auth = {
  getToken: () => string
  clearAuth: () => void
  refreshSession: () => Promise<boolean>
  hasAnyRole: (roles: readonly string[]) => boolean
  hasPermission: (permission: string) => boolean
}

export function loginLocation(fullPath: string) {
  const redirect = safeRedirect(fullPath)
  return { path: '/login', query: redirect ? { redirect } : {}, replace: true }
}

export function hasRouteAccess(
  to: Pick<Target, 'matched'>,
  auth: Pick<Auth, 'hasAnyRole' | 'hasPermission'>,
) {
  return to.matched.every(
    ({ meta }) =>
      (!meta.roles || auth.hasAnyRole(meta.roles)) &&
      (!meta.permission || auth.hasPermission(meta.permission)),
  )
}

export function createNavigationGuard(auth: Auth) {
  return async (to: Target) => {
    if (to.meta.idParam && !positiveId(to.params[to.meta.idParam]))
      return { path: '/404', replace: true }
    if (to.meta.articleQuery) {
      try {
        readArticleQuery(to.query || {})
      } catch {
        return { path: '/404', replace: true }
      }
    }
    if (to.meta.public) return true
    // 等待会话校验结果期间，用户可能已经退出或切换账号。
    for (let attempt = 0; attempt < 3; attempt++) {
      const token = auth.getToken()
      if (!tokenPayload(token)) {
        auth.clearAuth()
        return loginLocation(to.fullPath)
      }
      try {
        const current = await auth.refreshSession()
        if (auth.getToken() !== token || !current) continue
      } catch (error) {
        if (auth.getToken() !== token) continue
        const failure = error as { code?: number; response?: { status?: number } }
        if (failure.code === 401 || failure.response?.status === 401) {
          auth.clearAuth()
          return loginLocation(to.fullPath)
        }
        return {
          path: '/session-error',
          query: { redirect: safeRedirect(to.fullPath) || '/' },
          replace: true,
        }
      }
      if (!hasRouteAccess(to, auth)) return { path: '/403', replace: true }
      return true
    }
    return {
      path: '/session-error',
      query: { redirect: safeRedirect(to.fullPath) || '/' },
      replace: true,
    }
  }
}
