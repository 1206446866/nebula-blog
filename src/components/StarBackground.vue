<template>
  <div :id="id" class="bg"></div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, useId } from 'vue'
import type { Container } from '@tsparticles/engine'
import { initializedParticles } from '@/utils/particles'

const id = `nebula-stars-${useId()}`
let container: Container | undefined
let disposed = false

onBeforeUnmount(() => {
  disposed = true
  container?.destroy()
  container = undefined
})

onMounted(async () => {
  try {
    const engine = await initializedParticles()
    if (disposed) return
    const created = await engine.load({
      id,
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
    // 加载期间可能已经离开页面，迟到的实例也必须立即销毁。
    if (disposed) created?.destroy()
    else container = created
  } catch (error) {
    if (!disposed) console.warn('星空背景初始化失败', error)
  }
})
</script>

<style scoped>
.bg {
  position: fixed;
  inset: 0;
  z-index: 0;
}
</style>
