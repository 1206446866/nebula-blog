<script setup lang="ts">
import { toRef } from 'vue'
import { formatDateTime } from '@/utils/date'
import type { WritingDraft } from '@/api/article-workspace'
import { useArticleRevisions } from '../logic/useArticleRevisions'
const props = defineProps<{ id?: number; disabled: boolean }>()
const emit = defineEmits<{ restore: [draft: WritingDraft] }>()
const { visible, loading, failed, items, selected, draft, load, select } = useArticleRevisions(
  toRef(props, 'id'),
)
function restoreSelected() {
  if (!draft.value || loading.value || props.disabled) return
  emit('restore', draft.value)
  visible.value = false
}
</script>
<template>
  <el-button v-if="id" :disabled="disabled" @click="visible = true">版本历史</el-button>
  <el-dialog v-model="visible" title="文章版本历史" width="min(900px, 94vw)" append-to-body>
    <p class="revision-note">
      保留最近 30 份手动保存的版本。自动备份不产生历史版本，恢复只填回编辑器。
    </p>
    <p v-if="failed" role="alert">版本暂时无法读取 <el-button @click="load">重试</el-button></p>
    <div class="revision-layout">
      <div class="revision-list">
        <button
          v-for="item in items"
          :key="item.id"
          :aria-pressed="selected === item.id"
          @click="select(item.id)"
        >
          <strong>{{ item.title }}</strong
          ><small>版本 {{ item.version + 1 }} · {{ formatDateTime(item.createdAt) }}</small>
        </button>
        <p v-if="!loading && !failed && !items.length">
          还没有保存过版本，下次手动保存时会建立记录。
        </p>
      </div>
      <div class="revision-content">
        <p v-if="loading" role="status">正在读取…</p>
        <template v-else-if="draft"
          ><h3>{{ draft.title }}</h3>
          <pre>{{ draft.content }}</pre>
        </template>
        <p v-else>选择一个版本，查看当时的正文。</p>
      </div>
    </div>
    <template #footer
      ><el-button @click="visible = false">返回编辑器</el-button
      ><el-button type="primary" :disabled="!draft || loading || disabled" @click="restoreSelected"
        >恢复到编辑器</el-button
      ></template
    >
  </el-dialog>
</template>
<style scoped>
.revision-note {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 1.7;
}
.revision-layout {
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 20px;
  min-height: 240px;
}
.revision-list {
  max-height: 48vh;
  overflow: auto;
}
.revision-list button {
  display: block;
  width: 100%;
  text-align: left;
  padding: 12px;
  margin-bottom: 8px;
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  color: var(--el-text-color-primary);
  cursor: pointer;
}
.revision-list button[aria-pressed='true'] {
  border-color: var(--el-color-primary);
}
strong,
small {
  display: block;
  overflow-wrap: anywhere;
}
small {
  margin-top: 6px;
  color: var(--el-text-color-secondary);
  font-size: 11px;
}
.revision-content {
  max-height: 48vh;
  overflow: auto;
  padding: 0 14px;
}
.revision-content pre {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  font: 13px/1.9 monospace;
}
.revision-list button:focus-visible {
  outline: 2px solid var(--el-color-primary);
  outline-offset: 2px;
}
@media (max-width: 600px) {
  .revision-layout {
    grid-template-columns: 1fr;
  }
  .revision-list {
    max-height: 150px;
  }
  .revision-content {
    max-height: 32vh;
  }
}
</style>
