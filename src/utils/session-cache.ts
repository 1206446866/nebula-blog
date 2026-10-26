import { jwtDecode, type JwtPayload } from 'jwt-decode'
import type { UserVO } from '../api/user'

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

export function readAuthCache(storage: Pick<Storage, 'getItem'>) {
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
  if (!tokenPayload(token)) return empty
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
  return {
    token,
    user,
    roles: normalizeRoles(read('roles')),
    permissions: stringList(read('permissions')),
  }
}

export function persistAuth(
  storage: Pick<Storage, 'setItem' | 'removeItem'>,
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
    storage.setItem('token', state.token)
    storage.setItem('user', JSON.stringify(state.user))
    storage.setItem('roles', JSON.stringify(state.roles))
    storage.setItem('permissions', JSON.stringify(state.permissions))
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
