import { tokenPayload } from './session-cache'
import { useAuthStore } from '../stores/auth'

export function getTokenPayload() {
  const token = useAuthStore().getToken()
  if (!token) return null
  return tokenPayload(token)
}

export function getCurrentUserId() {
  return getTokenPayload()?.sub || -1
}
