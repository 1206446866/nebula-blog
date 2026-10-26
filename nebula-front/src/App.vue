<template>
  <RouterView v-slot="{ Component, route }">
    <component
      :is="Component"
      v-if="route.meta.public || canDisplay(route)"
      :key="route.meta.public ? 'public' : auth.viewRevision"
    />
    <main v-else class="session-check" role="status">正在确认登录状态…</main>
  </RouterView>
</template>

<script setup lang="ts">
import { RouterView, type RouteLocationNormalizedLoaded } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { hasRouteAccess } from '@/router/policy'

const auth = useAuthStore()
const canDisplay = (route: RouteLocationNormalizedLoaded) =>
  !!auth.verifiedToken && auth.verifiedToken === auth.getToken() && hasRouteAccess(route, auth)
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
