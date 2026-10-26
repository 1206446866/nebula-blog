<template>
  <div class="page admin-management-page">
    <div class="header admin-management-header admin-management-header--large">
      <h1>角色管理</h1>
      <p class="subtitle admin-management-subtitle admin-management-subtitle--small">
        管理系统角色与权限分配
      </p>
    </div>

    <div class="content admin-management-content admin-management-content--column">
      <!-- 查询区域 -->
      <el-form inline class="filter-form admin-management-filter">
        <el-form-item>
          <el-input v-model="descriptionFilter" placeholder="输入角色描述筛选" clearable />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="onSearch">搜索</el-button>
        </el-form-item>

        <el-form-item>
          <el-button type="success" @click="openCreate">新增角色</el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <BaseTable
        :data="roles"
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
        <!-- 操作 -->
        <template #actions="{ row }">
          <!-- 🔥 新增 -->
          <el-button size="small" type="warning" @click="openPermission(row)">{{
            row.name === 'SUPER_ADMIN' ? '查看权限' : '分配权限'
          }}</el-button>

          <el-button size="small" type="primary" @click="openEdit(row)"> 编辑 </el-button>

          <SafeButton
            size="small"
            type="danger"
            :disabled="[1, 2, 3].includes(row.id)"
            :onClick="() => deleteRole(row)"
          >
            删除
          </SafeButton>
        </template>
      </BaseTable>
    </div>

    <!-- 编辑/新增 -->
    <el-dialog
      class="admin-management-dialog"
      v-model="dialogVisible"
      :title="form.id ? '编辑角色' : '新增角色'"
      width="400px"
    >
      <el-form :model="form">
        <el-form-item label="角色名称">
          <el-input
            v-model="form.name"
            :disabled="form.id !== undefined && [1, 2, 3].includes(form.id)"
          />
        </el-form-item>

        <el-form-item label="描述">
          <el-input v-model="form.description" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <SafeButton type="primary" :onClick="submitForm">保存</SafeButton>
      </template>
    </el-dialog>

    <!--    分配权限对话框-->
    <RolePermissionDialog class="admin-management-dialog" ref="permDialogRef" />
  </div>
</template>

<script setup lang="ts">
import BaseTable from '@/components/basic/BaseTable.vue'
import SafeButton from '@/components/basic/SafeButton.vue'
import RolePermissionDialog from '@/components/dialog/RolePermissionDialog.vue'

import { useRoleManager } from './logic/useRoleManager'

const {
  descriptionFilter,
  dialogVisible,
  form,
  columns,
  roles,
  loading,
  tableError,
  current,
  size,
  total,
  reloadTable,
  onSearch,
  onPageChange,
  onSizeChange,
  permDialogRef,
  openCreate,
  openEdit,
  submitForm,
  deleteRole,
  openPermission,
} = useRoleManager()
</script>

<style src="./styles/management.css"></style>
