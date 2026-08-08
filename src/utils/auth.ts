import { jwtDecode } from 'jwt-decode'
import { useAuthStore } from '../stores/auth'

export function getTokenPayload() {
  const token = useAuthStore().getToken()
  if (!token) return null
  return jwtDecode(token)
}

export function getCurrentUserId() {
  return getTokenPayload()?.sub || -1
}
