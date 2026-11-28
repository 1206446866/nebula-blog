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
const canDisplay = (route: RouteLocationNormalizedLoaded) => {
  const token = auth.getToken()
  if (!token) return isGuestRoute(route)
  return auth.verifiedToken === token && hasRouteAccess(route, auth)
}
watch(() => auth.getToken(), (token, _, cleanup) => {
  if (!token) return
  let timer: ReturnType<typeof setTimeout> | undefined
  function checkExpiry() {
    if (auth.getToken() !== token) return
    const payload = tokenPayload(token)
    if (payload?.exp) {
      timer = setTimeout(checkExpiry, Math.min(2_147_483_647, Math.max(1, payload.exp * 1000 - Date.now())))
      return
    }
    auth.clearAuth()
    const route = router.currentRoute.value
    if (!route.meta.public && !isGuestRoute(route)) void router.replace(loginLocation(route.fullPath))
  }
  checkExpiry()
  cleanup(() => clearTimeout(timer))
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
