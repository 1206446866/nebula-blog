<template>
  <div class="page admin-management-page">
    <div class="header admin-management-header admin-management-header--large">
      <h1>权限管理</h1>
      <p class="subtitle admin-management-subtitle admin-management-subtitle--small">
        管理系统权限与功能点
      </p>
    </div>

    <div class="content admin-management-content admin-management-content--column">
      <!-- 查询区域 -->
      <el-form inline class="filter-form admin-management-filter">
        <el-form-item>
          <el-input v-model="nameFilter" placeholder="输入权限编码筛选" clearable />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="onSearch">搜索</el-button>
        </el-form-item>

        <el-form-item>
          <el-button type="success" @click="openCreate">新增权限</el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <BaseTable
        :data="permissions"
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
        <!-- 权限编码 -->
        <template #name="{ row }">
          {{ row.name }}
        </template>

        <!-- 描述 -->
        <template #description="{ row }">
          {{ row.description }}
        </template>

        <!-- 操作 -->
        <template #actions="{ row }">
          <el-button size="small" type="primary" @click="openEdit(row)"> 编辑 </el-button>

          <SafeButton size="small" type="danger" :onClick="() => remove(row)"> 删除 </SafeButton>
        </template>
      </BaseTable>
    </div>

    <!-- 编辑/新增 -->
    <el-dialog
      class="admin-management-dialog"
      v-model="dialogVisible"
      :title="form.id ? '编辑权限' : '新增权限'"
      width="400px"
    >
      <el-form :model="form">
        <el-form-item label="权限编码">
          <el-input v-model="form.name" />
        </el-form-item>

        <el-form-item label="描述">
          <el-input v-model="form.description" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <SafeButton type="primary" :onClick="submitForm"> 保存 </SafeButton>
      </template>
    </el-dialog>
  </div>
</template>
<script setup lang="ts">
import BaseTable from '@/components/basic/BaseTable.vue'
import SafeButton from '@/components/basic/SafeButton.vue'

import { usePermissionManager } from './logic/usePermissionManager'

const {
  nameFilter,
  dialogVisible,
  form,
  columns,
  permissions,
  loading,
  tableError,
  current,
  size,
  total,
  reloadTable,
  onSearch,
  onPageChange,
  onSizeChange,
  openCreate,
  openEdit,
  remove,
  submitForm,
} = usePermissionManager()
</script>

<style src="./styles/management.css"></style>
