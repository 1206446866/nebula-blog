<template>
  <div class="page admin-management-page">
    <div class="header admin-management-header admin-management-header--large">
      <h1>用户管理</h1>
      <p
        class="subtitle admin-management-subtitle admin-management-subtitle--small admin-management-subtitle--spaced"
      >
        管理平台用户信息与角色权限
      </p>
    </div>

    <div class="content admin-management-content admin-management-content--column">
      <!-- 查询区域 -->
      <el-form inline class="filter-form admin-management-filter">
        <el-form-item>
          <el-input v-model="roleFilter" placeholder="按角色ID、编码或名称筛选" clearable />
        </el-form-item>
        <el-form-item>
          <el-input v-model="userNameFilter" placeholder="输入用户名筛选" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch"> 搜索 </el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <BaseTable
        :data="users"
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
        <template #status="{ row }">
          <span :type="row.status === 0 ? 'success' : 'danger'">
            {{ row.status === 0 ? '正常' : '禁用' }}
          </span>
        </template>

        <template #roles="{ row }">
          {{
            row.roleIds
              ?.map((id: number) => roleOptions.find((r) => r.id === id)?.description)
              .filter(Boolean)
              .join('，')
          }}
        </template>

        <template #actions="{ row }">
          <span v-if="row.id === Number(getCurrentUserId())" type="info"> / </span>

          <span v-else-if="row.roleIds?.includes(1) && !isSuperAdmin" class="protected-account"
            >受保护的超级管理员</span
          >

          <template v-else>
            <el-button v-if="isSuperAdmin" size="small" type="primary" @click="editUser(row)">
              编辑
            </el-button>

            <el-button v-if="canResetPassword(row)" size="small" @click="openPasswordReset(row)">
              重置密码
            </el-button>

            <SafeButton
              size="small"
              :type="row.status === 0 ? 'danger' : 'success'"
              :onClick="() => switchStatus(row)"
            >
              {{ row.status === 0 ? '禁用' : '激活' }}
            </SafeButton>

            <SafeButton size="small" type="danger" :onClick="() => deleteUser(row)">
              删除
            </SafeButton>
          </template>
        </template>
      </BaseTable>
    </div>

    <!-- 编辑用户 -->
    <el-dialog
      class="admin-management-dialog"
      v-model="dialogVisible"
      title="编辑用户"
      width="min(400px, calc(100vw - 32px))"
    >
      <el-form :model="editForm">
        <p class="role-hint">超级管理员拥有全部权限。为管理员选择“超级管理员”角色即可提升权限。</p>
        <el-form-item label="用户名">
          <el-input v-model="editForm.username" />
        </el-form-item>

        <el-form-item label="角色">
          <el-select v-model="editForm.roleIds" multiple placeholder="请选择角色">
            <el-option
              v-for="item in roleOptions"
              :key="item.id"
              :label="item.description"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false"> 取消 </el-button>

        <SafeButton type="primary" :onClick="saveEdit"> 保存 </SafeButton>
      </template>
    </el-dialog>

    <el-dialog
      class="admin-management-dialog"
      v-model="passwordResetVisible"
      title="重置用户密码"
      width="min(440px, calc(100vw - 32px))"
      :close-on-click-modal="!passwordResetting"
      :close-on-press-escape="!passwordResetting"
      :show-close="!passwordResetting"
      @closed="clearPasswordReset"
    >
      <p class="role-hint">
        正在为「{{
          passwordResetTarget?.username
        }}」设置新密码。提交后该用户原有登录会话将失效，请通过安全渠道告知新密码。
      </p>
      <el-form label-position="top" @submit.prevent="submitPasswordReset">
        <el-form-item label="你的当前密码">
          <el-input
            v-model="passwordResetForm.operatorPassword"
            type="password"
            show-password
            autocomplete="current-password"
            :disabled="passwordResetting"
          />
        </el-form-item>
        <el-form-item label="用户的新密码">
          <el-input
            v-model="passwordResetForm.newPassword"
            type="password"
            show-password
            autocomplete="new-password"
            placeholder="6至64个字符"
            :disabled="passwordResetting"
          />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input
            v-model="passwordResetForm.confirmPassword"
            type="password"
            show-password
            autocomplete="new-password"
            :disabled="passwordResetting"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="passwordResetting" @click="passwordResetVisible = false"
          >取消</el-button
        >
        <el-button type="primary" :loading="passwordResetting" @click="submitPasswordReset"
          >确认重置</el-button
        >
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import BaseTable from '@/components/basic/BaseTable.vue'
import SafeButton from '@/components/basic/SafeButton.vue'
import { getCurrentUserId } from '@/utils/auth'

import { useUserManager } from './logic/useUserManager'

const {
  isSuperAdmin,
  passwordResetVisible,
  passwordResetting,
  passwordResetTarget,
  passwordResetForm,
  canResetPassword,
  clearPasswordReset,
  openPasswordReset,
  submitPasswordReset,
  roleFilter,
  userNameFilter,
  dialogVisible,
  editForm,
  columns,
  roleOptions,
  users,
  loading,
  tableError,
  current,
  size,
  total,
  reloadTable,
  onSearch,
  onPageChange,
  onSizeChange,
  switchStatus,
  editUser,
  saveEdit,
  deleteUser,
} = useUserManager()
</script>

<style src="./styles/management.css"></style>

<style scoped>
.role-hint {
  margin: 0 0 18px;
  color: #94a3b8;
  font-size: 13px;
  line-height: 1.6;
}
.protected-account {
  color: #a5b4fc;
  font-size: 12px;
}

.status-action span {
  display: flex;
  justify-content: center;
  margin-left: 7%;
  margin-right: 7%;
}

.status-action el-tag {
  display: inline-block; /* 确保不撑开 */
  padding: 0 6px; /* 调整内边距，更像文本 */
  font-size: 12px;
  cursor: default; /* 避免鼠标悬停像可点击 */
  line-height: 20px;
}
</style>
