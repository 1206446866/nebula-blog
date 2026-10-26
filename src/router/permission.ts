import router from './index'
import { useAuthStore } from '@/stores/auth'
import { createNavigationGuard } from './policy'

router.beforeEach((to) => createNavigationGuard(useAuthStore())(to))

window.addEventListener('storage', (event) => {
  if (event.key !== 'token' && event.key !== null) return
  useAuthStore().syncFromStorage()
  const current = router.currentRoute.value
  if (!current.meta.public)
    void router.replace({
      path: current.path,
      query: current.query,
      hash: current.hash,
      force: true,
    })
})
