import { loginLocation } from '@/router/policy'
import { useAuthStore } from '@/stores/auth'
import { useRoute, useRouter } from 'vue-router'

/** 交互前检查会话，登录返回后由用户重新操作。 */
export function useLoginPrompt() {
  const auth = useAuthStore()
  const route = useRoute()
  const router = useRouter()
  const goLogin = () => router.push(loginLocation(route.fullPath))
  const requireLogin = () => {
    if (auth.getToken()) return true
    void goLogin()
    return false
  }
  return { goLogin, requireLogin }
}
