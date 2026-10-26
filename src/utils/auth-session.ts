/** 先撤销操作开始时的会话，再清理本地状态；保留期间新登录的会话。 */
export async function revokeCurrentSession(options: {
  token: string
  revoke: (token: string) => Promise<unknown>
  currentToken: () => string
  clear: () => void
}): Promise<boolean> {
  const { token, revoke, currentToken, clear } = options
  if (token) {
    try {
      await revoke(token)
    } catch (error) {
      const failure = error as { response?: { status?: number }; code?: number }
      // 会话已过期或已被撤销时，无需再次处理。
      if (failure?.response?.status !== 401 && failure?.code !== 401) throw error
    }
  }
  if (currentToken() !== token) return false
  clear()
  return true
}

export function belongsToCurrentSession(authorization: unknown, token: string): boolean {
  return !!token && authorization === `Bearer ${token}`
}
