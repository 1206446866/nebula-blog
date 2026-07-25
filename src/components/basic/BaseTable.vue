<template>
  <div class="base-table">
    <el-table :data="data" :loading="loading" border style="width: 100%" v-bind="tableProps">
      <!-- 动态列 -->
      <el-table-column v-for="col in columns" :key="col.prop || col.label" v-bind="col">
        <!--        <template v-if="col.slot" #default="{ row }">-->
        <!--          <slot :name="col.slot" :row="row" />-->
        <!--        </template>-->
        <template v-if="col.slot" #default="scope">
          <slot :name="col.slot" v-bind="scope" />
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <el-pagination
      v-if="props.total ?? 0 > 0"
      class="pagination-container"
      :current-page="props.current"
      :page-size="props.size"
      :total="props.total"
      layout="total, sizes, prev, pager, next, jumper"
      :page-sizes="pageSizes"
      @current-change="onPageChange"
      @size-change="onSizeChange"
    />
  </div>
</template>

<script setup lang="ts">
import { defineEmits, defineProps } from 'vue'

interface Column {
  label: string
  prop?: string
  width?: string | number
  slot?: string
  [key: string]: any
}

const props = defineProps<{
  data: any[]
  columns: Column[]
  loading?: boolean
  current?: number
  size?: number
  total?: number
  pageSizes?: number[]
  tableProps?: Record<string, any>
}>()

const emit = defineEmits(['page-change', 'size-change'])

const onPageChange = (page: number) => {
  emit('page-change', Number(page))
}

const onSizeChange = (newSize: number) => {
  emit('size-change', Number(newSize))
}
</script>

<style scoped>
.base-table {
  width: 100%;
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

/* sizes 选择器 */
:deep(.el-pagination .el-select__wrapper) {
  background: rgba(31, 41, 55, 0.6) !important;
  border-radius: 6px;
  border: 1px solid rgba(148, 163, 184, 0.15);
}

/* 作用于分页 page-size 下拉弹层（teleport 到 body） */
:global(.el-select__popper) {
  background: rgba(17, 24, 39, 0.95) !important;
  border: 1px solid rgba(148, 163, 184, 0.2);
  backdrop-filter: blur(12px);
  border-radius: 6px;
}

/* 下拉项默认背景和文字颜色 */
:global(.el-select__popper .el-select-dropdown__item) {
  background-color: rgba(31, 41, 55, 0.95);
  color: #e5e7eb;
}

/* hover 高亮 */
:global(.el-select-dropdown__item.hover),
:global(.el-select-dropdown__item:hover) {
  background: rgba(59, 130, 246, 0.2);
}

/* 当前选中项 */
:global(.el-select-dropdown__item.selected) {
  color: #3b82f6;
}
</style>
