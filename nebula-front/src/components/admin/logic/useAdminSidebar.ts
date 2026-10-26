import { useAuthStore } from '@/stores/auth'

/** 管理后台菜单权限的状态与业务交互。 */
export function useAdminSidebar() {
  const authStore = useAuthStore()

  return { authStore }
}
