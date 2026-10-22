<template>
  <div class="content-card" id="article-content">
    <div class="markdown-body" v-html="htmlContent" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { renderMarkdown } from '@/utils/markdown'
import '@/styles/markdown.css'

const props = defineProps<{ content: string }>()
const htmlContent = computed(() => renderMarkdown(props.content))
</script>

<style scoped>
.content-card {
  background: rgba(255, 255, 255, 0.04);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 20px;
  padding: 40px;
}
@media (max-width: 768px) {
  .content-card {
    padding: 24px 18px;
  }
}
</style>
