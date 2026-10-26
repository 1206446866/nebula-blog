<template>
  <div class="page admin-management-page">
    <div class="header admin-management-header">
      <h1>标签管理</h1>
      <p class="subtitle admin-management-subtitle">管理系统标签信息</p>
    </div>

    <div class="content admin-management-content">
      <!-- 搜索 -->
      <el-form inline class="filter-form admin-management-filter">
        <el-form-item label="标签名称">
          <el-input v-model="nameFilter" placeholder="输入标签名称" clearable />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="onSearch">查询</el-button>
        </el-form-item>

        <el-form-item>
          <el-button type="success" @click="openCreate">新增标签</el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <BaseTable
        :data="tags"
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
        <template #actions="{ row }">
          <el-button size="small" type="primary" @click="openEdit(row)"> 编辑 </el-button>

          <el-button size="small" type="danger" @click="deleteTag(row)"> 删除 </el-button>
        </template>
      </BaseTable>

      <!-- 弹窗 -->
      <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
        <el-form :model="tagForm" label-width="80px">
          <el-form-item label="名称">
            <el-input v-model="tagForm.name" />
          </el-form-item>

          <el-form-item label="描述">
            <el-input v-model="tagForm.description" type="textarea" :rows="3" />
          </el-form-item>
        </el-form>

        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveTag">保存</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup lang="ts">
import BaseTable from '@/components/basic/BaseTable.vue'

import { useTagManager } from './logic/useTagManager'

const {
  nameFilter,
  dialogVisible,
  dialogTitle,
  tagForm,
  tags,
  loading,
  tableError,
  current,
  size,
  total,
  reloadTable,
  onSearch,
  onPageChange,
  onSizeChange,
  columns,
  openCreate,
  openEdit,
  saveTag,
  deleteTag,
} = useTagManager()
</script>

<style src="./styles/management.css"></style>
