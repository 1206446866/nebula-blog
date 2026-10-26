<template>
  <div class="comment-entry" :class="{ 'is-reply': reply }">
    <el-avatar :size="reply ? 32 : 40" :src="avatarUrl" class="avatar">{{
      displayName.charAt(0)
    }}</el-avatar>
    <div class="entry-body">
      <div class="entry-heading">
        <span class="name">{{ displayName }}</span
        ><span v-if="comment.userId === authorId" class="author-badge">作者</span>
        <span v-if="reply" class="reply-target"
          >回复 <span>@{{ comment.replyToUsername || '已注销用户' }}</span></span
        >
        <time :datetime="comment.createTime">{{ formatDateTime(comment.createTime) || '—' }}</time>
      </div>
      <p class="content">{{ comment.content }}</p>
      <div class="entry-actions">
        <button
          type="button"
          :aria-pressed="liked"
          :disabled="liking || disabled || !auth.hasPermission('comment:update')"
          :class="{ liked }"
          @click="toggleLike"
        >
          <svg
            class="like-icon"
            viewBox="0 0 24 24"
            aria-hidden="true"
            :fill="liked ? 'currentColor' : 'none'"
          >
            <path
              d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21l7.78-7.55 1.06-1.06a5.5 5.5 0 0 0 0-7.78Z"
            /></svg
          ><span>{{ count || '喜欢' }}</span>
        </button>
        <button
          v-if="auth.hasPermission('comment:create')"
          type="button"
          :disabled="disabled"
          @click="emit('reply', comment)"
        >
          <el-icon><ChatLineRound /></el-icon>回复
        </button>
        <button
          v-if="canDelete"
          type="button"
          class="delete"
          :disabled="disabled"
          @click="emit('delete', comment)"
        >
          删除
        </button>
      </div>
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ChatLineRound } from '@element-plus/icons-vue'
import { type CommentVO } from '@/api/comment'
import { formatDateTime } from '@/utils/date'

import { useCommentEntry } from '../logic/useCommentEntry'

const props = defineProps<{
  comment: CommentVO
  authorId?: number
  reply?: boolean
  disabled?: boolean
}>()
const emit = defineEmits<{ reply: [comment: CommentVO]; delete: [comment: CommentVO] }>()

const { auth, displayName, avatarUrl, canDelete, liked, count, liking, toggleLike } =
  useCommentEntry(props, emit)
</script>

<style scoped>
.comment-entry {
  display: flex;
  gap: 14px;
}
.avatar {
  flex-shrink: 0;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.3), rgba(56, 189, 248, 0.14));
  color: #c7d2fe;
  font-weight: 600;
}
.entry-body {
  min-width: 0;
  flex: 1;
}
.entry-heading {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  min-height: 32px;
}
.name {
  font-size: 14px;
  font-weight: 600;
  color: #dbeafe;
}
.author-badge {
  font-size: 10px;
  color: #a5b4fc;
  background: rgba(129, 140, 248, 0.13);
  padding: 2px 6px;
  border-radius: 5px;
}
time {
  margin-left: auto;
  font-size: 11px;
  color: #8290ab;
}
.reply-target {
  font-size: 12px;
  color: #7e8da8;
}
.reply-target span {
  color: #a5b4fc;
}
.content {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  font-size: 14px;
  line-height: 1.9;
  color: #cbd5e1;
  margin: 8px 0 12px;
}
.entry-actions {
  display: flex;
  gap: 20px;
  align-items: center;
}
.entry-actions button {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 0;
  border: 0;
  background: transparent;
  color: #8d9bb6;
  font: inherit;
  font-size: 12px;
  cursor: pointer;
}
.entry-actions button:hover,
.entry-actions .liked {
  color: #a5b4fc;
}
.like-icon {
  width: 14px;
  height: 14px;
  stroke: currentColor;
  stroke-width: 1.6;
  stroke-linejoin: round;
}
.entry-actions button:disabled {
  cursor: default;
  opacity: 0.55;
}
.entry-actions .delete {
  margin-left: auto;
}
.entry-actions .delete:hover {
  color: #fda4af;
}
.is-reply .content {
  font-size: 13px;
  margin-top: 5px;
}
.is-reply .name {
  font-size: 13px;
}
@media (max-width: 600px) {
  .comment-entry {
    gap: 10px;
  }
  time {
    flex-basis: 100%;
    margin-left: 0;
  }
  .entry-heading {
    gap: 5px;
  }
  .content {
    font-size: 13px;
  }
}
</style>
