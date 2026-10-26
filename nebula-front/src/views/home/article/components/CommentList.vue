<template>
  <div class="comment-list">
    <CommentThread
      v-for="item in list"
      :key="item.id"
      :comment="item"
      :article-id="articleId"
      :author-id="authorId"
      @removed="emit('refresh')"
    />
  </div>
</template>
<script setup lang="ts">
import type { CommentVO } from '@/api/comment'
import CommentThread from './CommentThread.vue'
defineProps<{ list: CommentVO[]; articleId: number; authorId?: number }>()
const emit = defineEmits<{ refresh: [] }>()
</script>
<style scoped>
.comment-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
</style>
