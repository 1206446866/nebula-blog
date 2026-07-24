<template>
  <div class="pagination-wrapper">
    <el-pagination
      background
      layout="prev, pager, next, jumper"
      :current-page="currentPage"
      :page-size="pageSize"
      :total="total"
      @current-change="handleChange"
    />
  </div>
</template>

<script setup lang="ts">
interface Props {
  currentPage: number
  pageSize: number
  total: number
}

defineProps<Props>()

const emit = defineEmits<{
  (e: 'update:currentPage', value: number): void
}>()

const handleChange = (page: number) => {
  emit('update:currentPage', page)
}
</script>

<style scoped>
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 45px;
  padding-bottom: 40px;
}

/* 整体 */
:deep(.el-pagination) {
  --el-pagination-bg-color: rgba(255, 255, 255, 0.05);
  --el-pagination-button-color: rgba(255, 255, 255, 0.7);
  --el-pagination-hover-color: #78b4ff;
  color: rgba(255, 255, 255, 0.7);
}

/* 上一页 / 下一页按钮 */
:deep(.el-pagination button) {
  background: rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.75);
  border-radius: 8px;
  transition: 0.25s;
  border: 1px solid rgba(255, 255, 255, 0.08);
}

:deep(.el-pagination button:hover) {
  color: #78b4ff;
  background: rgba(120, 180, 255, 0.12);
  border-color: rgba(120, 180, 255, 0.35);
}
/* 页码 */
:deep(.el-pager li) {
  background: rgba(255, 255, 255, 0.05);

  color: rgb(13 28 191 / 0.65);

  border-radius: 8px;

  border: 1px solid rgba(255, 255, 255, 0.08);

  transition: 0.25s;
}

:deep(.el-pager li:hover) {
  color: #78b4ff;

  background: rgba(120, 180, 255, 0.12);
}

/* 当前页 */
:deep(.el-pager li.is-active) {
  color: #78b4ff;

  background: rgba(120, 180, 255, 0.18);

  border-color: rgba(120, 180, 255, 0.45);

  box-shadow: 0 0 12px rgba(120, 180, 255, 0.25);
}

/* 跳转输入框 */
:deep(.el-pagination__jump) {
  color: rgba(255, 255, 255, 0.55);
}

:deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.05);

  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.08) inset;

  border-radius: 8px;
}

:deep(.el-input__inner) {
  color: rgba(255, 255, 255, 0.85);
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(120, 180, 255, 0.35) inset;
}

:deep(.el-pagination .btn-prev:disabled),
:deep(.el-pagination .btn-next:disabled) {
  opacity: 0.35;
}
</style>
