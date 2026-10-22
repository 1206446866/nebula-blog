/** Revoke the captured session before clearing it; a newer login must survive. */
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
      // An expired or already revoked session has already reached the desired state.
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
