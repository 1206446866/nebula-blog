<template>
  <div class="comment-editor" :class="{ compact: parentId }">
    <div class="editor-heading">
      <span>{{ parentId ? `回复 @${replyTo || '已注销用户'}` : '留下你的想法' }}</span>
      <button v-if="parentId" type="button" :disabled="submitting" @click="emit('cancel')">
        取消回复
      </button>
      <span v-else class="editor-note">让有趣的想法，在这里相遇</span>
    </div>
    <el-input
      ref="input"
      v-model="content"
      type="textarea"
      :rows="parentId ? 3 : 4"
      :maxlength="2000"
      show-word-limit
      :disabled="submitting || !canWrite || disabled"
      :placeholder="
        canWrite
          ? parentId
            ? '接着这个话题聊聊…'
            : '写下你的观点、问题，或者一句喜欢…'
          : '当前账号暂无评论权限'
      "
      @keydown.ctrl.enter.prevent="submit"
      @keydown.meta.enter.prevent="submit"
    />
    <div class="editor-actions">
      <span>友善交流 · Ctrl / ⌘ + Enter 发送</span>
      <el-button
        type="primary"
        :loading="submitting"
        :disabled="disabled || !canWrite || !content.trim()"
        @click="submit"
      >
        {{ parentId ? '发送回复' : '发表评论' }}
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useCommentEditor } from '../logic/useCommentEditor'

const props = defineProps<{
  articleId: number
  parentId?: number
  replyTo?: string
  disabled?: boolean
}>()
const emit = defineEmits<{ success: []; cancel: []; busy: [value: boolean] }>()

const { canWrite, content, submitting, input, submit } = useCommentEditor(props, emit)
</script>

<style scoped>
.comment-editor {
  padding: 22px;
  border: 1px solid rgba(129, 140, 248, 0.2);
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.09), rgba(56, 189, 248, 0.035));
}
.comment-editor.compact {
  margin: 16px 0 6px;
  padding: 16px;
  border-radius: 12px;
}
.editor-heading,
.editor-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.editor-heading {
  margin-bottom: 14px;
  color: #dbeafe;
  font-size: 14px;
  font-weight: 600;
}
.editor-note,
.editor-actions > span {
  color: #8d9bb6;
  font-size: 12px;
  font-weight: 400;
}
.editor-heading button {
  border: 0;
  background: transparent;
  color: #a5b4fc;
  cursor: pointer;
  font-size: 12px;
}
.editor-actions {
  margin-top: 14px;
}
.comment-editor :deep(.el-textarea__inner) {
  background: rgba(9, 15, 30, 0.5);
  color: #e2e8f0;
  border-radius: 12px;
  padding: 14px;
  box-shadow: 0 0 0 1px rgba(148, 163, 184, 0.14) inset;
}
.comment-editor :deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 1px #818cf8 inset;
}
.comment-editor :deep(.el-input__count) {
  background: transparent;
  color: #7886a2;
  bottom: 6px;
  right: 12px;
}
.editor-actions :deep(.el-button) {
  border: 0;
  border-radius: 10px;
  background: linear-gradient(120deg, #6366f1, #3b82f6);
}
@media (max-width: 600px) {
  .comment-editor {
    padding: 16px;
  }
  .editor-note {
    display: none;
  }
  .editor-actions > span {
    font-size: 10px;
  }
}
</style>
