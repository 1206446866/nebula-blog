<template>
  <div class="page admin-management-page">
    <div class="header admin-management-header">
      <h1>评论管理</h1>
      <p class="subtitle admin-management-subtitle">管理平台评论内容与互动信息</p>
    </div>

    <div class="content admin-management-content">
      <!-- 查询区域 -->
      <el-form inline class="filter-form">
        <el-form-item>
          <el-input v-model.number="articleIdFilter" placeholder="请输入文章ID" clearable />
        </el-form-item>
        <el-form-item>
          <el-input v-model="contentFilter" placeholder="请输入评论内容" clearable />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="onSearch"> 搜索 </el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <BaseTable
        :data="comments"
        :columns="columns"
        :loading="loading"
        :error="tableError"
        @retry="reloadTable"
        :current="current"
        :size="size"
        :total="total"
        @page-change="onPageChange"
        @size-change="onSizeChange"
      >
        <template #content="{ row }">
          <el-tooltip :content="row.content" placement="top">
            <span class="ellipsis">
              {{ row.content }}
            </span>
          </el-tooltip>
        </template>

        <template #createTime="{ row }">
          {{ formatDateTime(row.createTime) || '—' }}
        </template>

        <template #actions="{ row }">
          <SafeButton size="small" type="danger" :onClick="() => deleteComment(row)">
            删除
          </SafeButton>
        </template>
      </BaseTable>
    </div>
  </div>
</template>

<script setup lang="ts">
import BaseTable from '@/components/basic/BaseTable.vue'
import SafeButton from '@/components/basic/SafeButton.vue'
import { formatDateTime } from '@/utils/date'

import { useCommentManager } from './logic/useCommentManager'

const {
  articleIdFilter,
  contentFilter,
  columns,
  comments,
  loading,
  tableError,
  current,
  size,
  total,
  reloadTable,
  onSearch,
  onPageChange,
  onSizeChange,
  deleteComment,
} = useCommentManager()
</script>

<style src="./styles/management.css"></style>

<style scoped>
.ellipsis {
  display: inline-block;
  max-width: 500px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
