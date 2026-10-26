<template>
  <div class="base-table">
    <div v-if="error" class="table-error" role="alert">
      <span>{{ error }}</span>
      <el-button link type="primary" :disabled="loading" @click="emit('retry')">重新加载</el-button>
    </div>
    <el-table v-loading="loading" v-bind="tableProps" :data="data" border style="width: 100%">
      <!-- 动态列 -->
      <el-table-column v-for="col in columns" :key="col.prop || col.label" v-bind="col">
        <template v-if="col.slot" #default="scope">
          <slot :name="col.slot" v-bind="scope" />
        </template>
      </el-table-column>
      <template #empty>{{
        loading ? '正在加载…' : error ? '数据暂时未能加载' : '暂无数据'
      }}</template>
    </el-table>

    <!-- 分页 -->
    <el-pagination
      v-if="total > 0"
      class="pagination-container"
      :current-page="props.current"
      :page-size="props.size"
      :total="props.total"
      layout="total, sizes, prev, pager, next, jumper"
      :page-sizes="pageSizes"
      :disabled="loading"
      popper-class="nebula-table-pagination"
      @current-change="onPageChange"
      @size-change="onSizeChange"
    />
  </div>
</template>

<script setup lang="ts" generic="Row extends object">
import type { TableColumnCtx } from 'element-plus'
import type { VNode } from 'vue'
import type { TableColumn } from './table'

const props = withDefaults(
  defineProps<{
    data: Row[]
    columns: TableColumn<Row>[]
    loading?: boolean
    error?: string | null
    current?: number
    size?: number
    total?: number
    pageSizes?: number[]
    tableProps?: Record<string, unknown>
  }>(),
  {
    loading: false,
    error: null,
    current: 1,
    size: 10,
    total: 0,
    pageSizes: () => [10, 20, 50, 100],
  },
)

const emit = defineEmits<{
  'page-change': [page: number]
  'size-change': [size: number]
  retry: []
}>()

defineSlots<{
  [name: string]: (scope: { row: Row; column: TableColumnCtx<Row>; $index: number }) => VNode[]
}>()

const onPageChange = (page: number) => {
  if (!props.loading && Number.isSafeInteger(page) && page > 0) emit('page-change', page)
}

const onSizeChange = (newSize: number) => {
  if (!props.loading && Number.isSafeInteger(newSize) && newSize > 0) emit('size-change', newSize)
}
</script>

<style scoped>
.base-table {
  width: 100%;
}

.table-error {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  color: #fca5a5;
}

:deep(.el-table) {
  --el-table-bg-color: rgba(15, 23, 42, 0.45);
  --el-table-tr-bg-color: rgba(15, 23, 42, 0.45);
  --el-table-border-color: rgba(148, 163, 184, 0.15);
  backdrop-filter: blur(12px);
  border-radius: 12px;
}

:deep(.el-table th.el-table__cell) {
  background: rgba(17, 24, 39, 0.6);
}

/* 列颜色 */
:deep(.el-table__body tr:hover > td.el-table__cell) {
  background: rgba(59, 130, 246, 0.12) !important;
  transition: background-color 0.2s;
}

.pagination-container {
  margin-top: auto;
  display: flex;
  justify-content: center;
  padding: 16px 0;
}

/* 页码按钮 */
:deep(.el-pagination .btn-prev),
:deep(.el-pagination .btn-next),
:deep(.el-pagination .el-pager li) {
  background: transparent;
  color: #e5e7eb;
}

/* 当前选中页 */
:deep(.el-pagination .el-pager li.is-active) {
  background: #3b82f6;
  color: #fff;
  border-radius: 6px;
}

/* 每页条数选择器 */
:deep(.el-pagination .el-select__wrapper) {
  background: rgba(31, 41, 55, 0.6) !important;
  border-radius: 6px;
  border: 1px solid rgba(148, 163, 184, 0.15);
}

/* 每页条数的下拉浮层会移到页面根节点，需使用专用样式类。 */
:global(.nebula-table-pagination) {
  background: rgba(17, 24, 39, 0.95) !important;
  border: 1px solid rgba(148, 163, 184, 0.2);
  backdrop-filter: blur(12px);
  border-radius: 6px;
}

/* 下拉项默认背景和文字颜色 */
:global(.nebula-table-pagination .el-select-dropdown__item) {
  background-color: rgba(31, 41, 55, 0.95);
  color: #e5e7eb;
}

/* 悬停高亮 */
:global(.nebula-table-pagination .el-select-dropdown__item.hover),
:global(.nebula-table-pagination .el-select-dropdown__item:hover) {
  background: rgba(59, 130, 246, 0.2);
}

/* 当前选中项 */
:global(.nebula-table-pagination .el-select-dropdown__item.selected) {
  color: #3b82f6;
}
</style>
