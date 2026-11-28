<template>
  <RouterView v-slot="{ Component, route }">
    <component
      :is="Component"
      v-if="route.meta.public || canDisplay(route)"
      :key="route.meta.public ? 'public' : auth.getToken() ? auth.viewRevision : 'guest'"
    />
    <main v-else class="session-check" role="status">正在确认登录状态…</main>
  </RouterView>
</template>

<script setup lang="ts">
import { RouterView, useRouter, type RouteLocationNormalizedLoaded } from 'vue-router'
import { watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { hasRouteAccess, isGuestRoute, loginLocation } from '@/router/policy'
import { tokenPayload } from '@/utils/session-cache'

const auth = useAuthStore()
const router = useRouter()
const sessionCheckIntervalMs = 30_000
const canDisplay = (route: RouteLocationNormalizedLoaded) => {
  const token = auth.getToken()
  if (!token) return isGuestRoute(route)
  return auth.verifiedToken === token && hasRouteAccess(route, auth)
}
watch(() => auth.getToken(), (token, _, cleanup) => {
  if (!token) return
  let expiryTimer: ReturnType<typeof setTimeout> | undefined
  let checking = false

  function endSession() {
    if (auth.getToken() !== token) return
    auth.clearAuth()
    const route = router.currentRoute.value
    if (!route.meta.public && !isGuestRoute(route)) void router.replace(loginLocation(route.fullPath))
  }

  function checkExpiry() {
    if (auth.getToken() !== token) return
    const payload = tokenPayload(token)
    if (payload?.exp) {
      expiryTimer = setTimeout(checkExpiry, Math.min(2_147_483_647, Math.max(1, payload.exp * 1000 - Date.now())))
      return
    }
    endSession()
  }

  async function recheckSession() {
    if (document.hidden || checking || auth.getToken() !== token || auth.verifiedToken !== token) return
    checking = true
    try {
      if (!(await auth.refreshSession()) || auth.getToken() !== token) return
      const route = router.currentRoute.value
      if (!route.meta.public && !hasRouteAccess(route, auth)) void router.replace('/403')
    } catch (error) {
      // 公开接口可匿名返回，只有会话接口确认失效时才清除身份。
      const failure = error as { code?: number; response?: { status?: number } } | undefined
      if (failure?.code === 401 || failure?.response?.status === 401) endSession()
    } finally {
      checking = false
    }
  }

  checkExpiry()
  const recheckTimer = setInterval(recheckSession, sessionCheckIntervalMs)
  document.addEventListener('visibilitychange', recheckSession)
  cleanup(() => {
    clearTimeout(expiryTimer)
    clearInterval(recheckTimer)
    document.removeEventListener('visibilitychange', recheckSession)
  })
}, { immediate: true })
</script>

<style scoped>
.session-check {
  min-height: 100dvh;
  display: grid;
  place-items: center;
  background: #020617;
  color: #94a3b8;
}
</style>
