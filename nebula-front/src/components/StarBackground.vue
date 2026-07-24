<template>
  <div id="tsparticles" class="bg"></div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { tsParticles } from '@tsparticles/engine'
import { loadFull } from 'tsparticles'

onMounted(async () => {
  await loadFull(tsParticles)

  // ⚠️ 关键：只传 1 个参数（id + config 合并写法）
  tsParticles.load({
    id: 'tsparticles',
    options: {
      fullScreen: {
        enable: true,
        zIndex: 0,
      },

      background: {
        color: '#020617',
      },

      fpsLimit: 60,

      particles: {
        number: { value: 80 },
        color: { value: '#38bdf8' },
        shape: { type: 'circle' },
        opacity: { value: 0.5 },
        size: { value: 2 },
        move: { enable: true, speed: 0.6 },
        links: {
          enable: true,
          distance: 120,
          color: '#818cf8',
          opacity: 0.3,
        },
      },

      detectRetina: true,
    },
  })
})
</script>

<style scoped>
.bg {
  position: fixed;
  inset: 0;
  z-index: 0;
}
</style>
