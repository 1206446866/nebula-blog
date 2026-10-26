<template><canvas ref="canvas" class="orbit-canvas" aria-hidden="true"></canvas></template>
<script setup lang="ts">
import { useTemplateRef } from 'vue'
import { useOrbitCanvas } from '../logic/useOrbitCanvas'
const props = defineProps<{ paused: boolean; pointerX: number; pointerY: number }>()
const canvas = useTemplateRef<HTMLCanvasElement>('canvas')
useOrbitCanvas(canvas, props)
</script>
<style scoped>
.orbit-canvas {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}
</style>
