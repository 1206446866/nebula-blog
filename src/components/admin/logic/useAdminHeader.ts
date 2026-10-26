import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'

/** 管理后台导航的状态与业务交互。 */
export function useAdminHeader() {
  const router = useRouter()
  const userStore = useAuthStore()
  const logout = async () => {
    if (await userStore.logout()) await router.replace('/login')
  }
  const profile = () => {
    if (userStore.user?.id) router.push('/user/' + userStore.user.id)
  }

  return { userStore, logout, profile }
}
