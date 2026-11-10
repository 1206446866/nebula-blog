import { jwtDecode, type JwtPayload } from 'jwt-decode'
import type { UserVO } from '../api/user'
import { pruneWritingCache } from './private-cache.ts'

export const normalizeRoles = (value: unknown): string[] =>
  Array.isArray(value)
    ? [
        ...new Set(
          value
            .filter((item): item is string => typeof item === 'string')
            .map((item) => item.replace(/^ROLE_/, '')),
        ),
      ]
    : []

export const stringList = (value: unknown): string[] =>
  Array.isArray(value) ? value.filter((item): item is string => typeof item === 'string') : []

export const authorizationKey = (roles: readonly string[], permissions: readonly string[]) =>
  JSON.stringify([[...new Set(roles)].sort(), [...new Set(permissions)].sort()])

export function tokenPayload(token: unknown, now = Date.now()): JwtPayload | null {
  if (typeof token !== 'string' || !token) return null
  try {
    const payload = jwtDecode<JwtPayload>(token)
    if (
      !payload ||
      typeof payload !== 'object' ||
      typeof payload.sub !== 'string' ||
      !/^[1-9]\d*$/.test(payload.sub) ||
      !Number.isSafeInteger(Number(payload.sub)) ||
      typeof payload.exp !== 'number' ||
      !Number.isFinite(payload.exp) ||
      payload.exp * 1000 <= now
    )
      return null
    return payload
  } catch {
    return null
  }
}

/** 仅用于清理对应账号的本机缓存，不作为服务端身份或权限依据。 */
export function tokenAccountId(token: unknown): number | undefined {
  try {
    const sub = jwtDecode<JwtPayload>(String(token || '')).sub
    const id = typeof sub === 'string' && /^[1-9]\d*$/.test(sub) ? Number(sub) : NaN
    return Number.isSafeInteger(id) ? id : undefined
  } catch {
    return undefined
  }
}

export function readAuthCache(
  storage: Pick<Storage, 'getItem' | 'removeItem' | 'length' | 'key'>,
  cleanupInvalid = true,
) {
  pruneWritingCache(storage)
  const empty = {
    token: '',
    user: null as UserVO | null,
    roles: [] as string[],
    permissions: [] as string[],
  }
  const read = (key: string): unknown => {
    try {
      const value = storage.getItem(key)
      return value ? JSON.parse(value) : null
    } catch {
      return null
    }
  }
  let token = ''
  try {
    token = storage.getItem('token') || ''
  } catch {
    return empty
  }
  // 另一个标签页可能正在先清旧Token、再发布新会话；空Token只表示暂未登录。
  if (!token) return empty
  const value = read('user')
  const user =
    value &&
    typeof value === 'object' &&
    'id' in value &&
    Number.isSafeInteger(value.id) &&
    Number(value.id) > 0 &&
    'username' in value &&
    typeof value.username === 'string'
      ? (value as UserVO)
      : null
  if (!tokenPayload(token) || !user || String(user.id) !== tokenPayload(token)?.sub) {
    // 先移除共享Token，让其他标签页的备份写入立即失效。
    try {
      if (cleanupInvalid && storage.getItem('token') === token) persistAuth(storage, empty)
    } catch {
      /* 存储权限变化时仍以未登录状态返回。 */
    }
    // 自然过期不等于主动退出，未同步稿件仍供原账号重新登录后恢复。
    return empty
  }
  return {
    token,
    user,
    roles: normalizeRoles(read('roles')),
    permissions: stringList(read('permissions')),
  }
}

export function persistAuth(
  storage: Pick<Storage, 'removeItem'> & Partial<Pick<Storage, 'setItem'>>,
  state: {
    token: string
    user: UserVO | null
    roles: string[]
    permissions: string[]
  },
) {
  try {
    if (!state.token) {
      for (const key of ['token', 'user', 'roles', 'permissions']) storage.removeItem(key)
      return
    }
    storage.setItem?.('user', JSON.stringify(state.user))
    storage.setItem?.('roles', JSON.stringify(state.roles))
    storage.setItem?.('permissions', JSON.stringify(state.permissions))
    // Token最后写入，其他标签页收到通知时读取到完整的新账号缓存。
    storage.setItem?.('token', state.token)
  } catch {
    /* 浏览器无法持久保存会话时，仍可在当前页面内存中使用。 */
  }
}

export function singleFlight<T>(fetcher: (token: string) => Promise<T>) {
  const pending = new Map<string, Promise<T>>()
  return (token: string) => {
    const existing = pending.get(token)
    if (existing) return existing
    const request = Promise.resolve()
      .then(() => fetcher(token))
      .finally(() => {
        if (pending.get(token) === request) pending.delete(token)
      })
    pending.set(token, request)
    return request
  }
}
