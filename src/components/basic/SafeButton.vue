<template>
  <el-button
    :type="type"
    :size="size"
    :disabled="innerLoading || disabled"
    :loading="innerLoading"
    @click="handleClick"
  >
    <slot />
  </el-button>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const props = defineProps({
  type: { type: String, default: 'primary' },
  size: { type: String, default: 'default' },
  disabled: { type: Boolean, default: false },
  // 外部传入异步函数
  onClick: { type: Function, required: true },
})

const innerLoading = ref(false)

const handleClick = async () => {
  if (innerLoading.value) return

  innerLoading.value = true
  try {
    await props.onClick()
  } finally {
    innerLoading.value = false
  }
}
</script>
