<template>
  <div class="page admin-management-page">
    <div class="header admin-management-header">
      <h1>分类管理</h1>
      <p class="subtitle admin-management-subtitle">管理系统文章分类</p>
    </div>

    <div class="content admin-management-content">
      <!-- 搜索栏 -->
      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="请输入分类名称"
          clearable
          style="width: 240px"
          @keyup.enter="handleSearch"
        />

        <div class="toolbar-right">
          <el-button @click="handleSearch"> 搜索 </el-button>

          <el-button type="primary" @click="handleCreate"> 新增分类 </el-button>
        </div>
      </div>

      <!-- 表格 -->
      <BaseTable
        :data="categoryList"
        :columns="columns"
        :loading="loading"
        :error="tableError"
        @retry="reloadTable"
        :current="current"
        :size="size"
        :total="total"
        @page-change="handlePageChange"
        @size-change="handleSizeChange"
      >
        <template #action="{ row }">
          <el-button type="primary" link @click="handleEdit(row)"> 编辑 </el-button>
          <el-button type="danger" link @click="handleDelete(row)"> 删除 </el-button>
        </template>
      </BaseTable>
    </div>
  </div>

  <el-dialog v-model="dialogVisible" title="新增分类" width="500px">
    <el-form :model="createForm" label-width="80px">
      <el-form-item label="分类名称">
        <el-input v-model="createForm.name" placeholder="请输入分类名称" />
      </el-form-item>

      <el-form-item label="分类描述">
        <el-input
          v-model="createForm.description"
          type="textarea"
          :rows="3"
          placeholder="请输入分类描述"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="dialogVisible = false"> 取消 </el-button>

      <el-button type="primary" @click="handleSubmit"> 确定 </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import BaseTable from '@/components/basic/BaseTable.vue'

import { useCategoryManager } from './logic/useCategoryManager'

const {
  keyword,
  columns,
  dialogVisible,
  createForm,
  categoryList,
  loading,
  tableError,
  current,
  size,
  total,
  reloadTable,
  handleSearch,
  handlePageChange,
  handleSizeChange,
  handleCreate,
  handleSubmit,
  handleEdit,
  handleDelete,
} = useCategoryManager()
</script>

<style src="./styles/management.css"></style>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;

  margin-bottom: 16px;
}

.toolbar-right {
  display: flex;
  gap: 12px;
}
</style>
