<template>
  <div class="editor">
    <el-input v-model="content" type="textarea" :rows="3" placeholder="写下你的评论..." />

    <div class="actions">
      <el-button type="primary" @click="submit">发表评论</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { releaseCommentApi } from '@/api/comment'

const props = defineProps<{
  articleId: number
}>()

const emit = defineEmits(['success'])

const content = ref('')

const submit = async () => {
  if (!content.value.trim()) {
    ElMessage.warning('评论不能为空')
    return
  }

  const res = await releaseCommentApi({
    articleId: props.articleId,
    content: content.value,
  })

  if (res.code === 200 && res.data) {
    ElMessage.success('评论成功')
    content.value = ''
    emit('success')
  } else {
    ElMessage.error(res.msg || '评论失败')
  }
}
</script>

<style scoped>
.actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}
.editor {
  margin-top: 32px;
  padding: 14px;
  border-radius: 14px;

  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);

  backdrop-filter: blur(10px);
}

/* 输入框整体 */
.editor :deep(.el-textarea__inner) {
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.08);
  color: #e5e7eb;
  border-radius: 10px;
  padding: 10px 12px;

  transition: all 0.2s ease;
}

/* placeholder */
.editor :deep(.el-textarea__inner::placeholder) {
  color: #64748b;
}

/* hover */
.editor :deep(.el-textarea__inner:hover) {
  border-color: rgba(56, 189, 248, 0.4);
}

/* focus */
.editor :deep(.el-textarea__inner:focus) {
  border-color: #38bdf8;
  box-shadow: 0 0 0 2px rgba(56, 189, 248, 0.15);
}

/* 按钮区域 */
.actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}

/* 按钮轻微优化（Element Plus） */
.editor :deep(.el-button--primary) {
  background: linear-gradient(135deg, #38bdf8, #0ea5e9);
  border: none;
  border-radius: 10px;

  transition: all 0.2s ease;
}

.editor :deep(.el-button--primary:hover) {
  transform: translateY(-1px);
  opacity: 0.9;
}
</style>
