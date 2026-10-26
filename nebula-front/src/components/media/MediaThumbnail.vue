<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'
import { mediaFileApi } from '@/api/media'
const props = defineProps<{ id: number; name: string }>()
const url = ref('')
const failed = ref(false)
let controller: AbortController | undefined
let generation = 0
function release() {
  generation++
  controller?.abort()
  if (url.value) URL.revokeObjectURL(url.value)
  url.value = ''
}
watch(
  () => props.id,
  async (id) => {
    release()
    failed.value = false
    const run = generation
    controller = new AbortController()
    try {
      const blob = await mediaFileApi(id, controller.signal)
      if (run !== generation) return
      if (!blob.type.startsWith('image/')) throw new Error('Invalid image')
      url.value = URL.createObjectURL(blob)
    } catch {
      if (run === generation) failed.value = true
    }
  },
  { immediate: true },
)
onBeforeUnmount(release)
</script>
<template>
  <img v-if="url" :src="url" :alt="name" loading="lazy" />
  <span v-else class="image-state">{{ failed ? '图片不可用' : '载入星光…' }}</span>
</template>
<style scoped>
img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}
.image-state {
  color: #8d91ad;
  font-size: 12px;
}
</style>
