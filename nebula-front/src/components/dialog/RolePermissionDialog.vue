<template>
  <el-dialog
    v-model="visible"
    title="权限分配"
    width="min(700px, calc(100vw - 32px))"
    class="perm-dialog"
  >
    <el-alert
      v-if="isSuperAdminRole"
      title="超级管理员自动拥有全部权限，无需单独分配。"
      type="info"
      :closable="false"
      show-icon
      class="root-notice"
    />
    <!-- 角色信息 -->
    <el-card class="info-card">
      <div class="title">角色信息</div>
      <div class="row">角色：{{ role.name }}</div>
      <div class="row">描述：{{ role.description }}</div>
    </el-card>

    <!-- 模块切换 -->
    <div class="tabs">
      <el-button
        v-for="m in modules"
        :key="m.key"
        :type="currentModule === m.key ? 'primary' : 'default'"
        @click="currentModule = m.key"
      >
        {{ m.name }}
      </el-button>
    </div>

    <!-- 权限面板 -->
    <el-card class="perm-card">
      <div class="title">{{ currentModuleName }}</div>
      <el-checkbox-group
        v-model="checkedMap[currentModule]"
        class="perm-group"
        :disabled="isSuperAdminRole || submitting"
      >
        <el-checkbox v-for="p in currentPermissions" :key="p.id" :value="p.id" class="perm-item">
          <span class="perm-label" :title="p.name">
            <span>{{ p.description || p.name }}</span>
            <small v-if="p.description">{{ p.name }}</small>
          </span>
        </el-checkbox>
      </el-checkbox-group>
    </el-card>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button v-if="!isSuperAdminRole" type="primary" :loading="submitting" @click="submit"
        >保存</el-button
      >
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { useRolePermissionDialog } from './logic/useRolePermissionDialog'

const {
  visible,
  submitting,
  isSuperAdminRole,
  role,
  modules,
  currentModule,
  checkedMap,
  currentPermissions,
  currentModuleName,
  open,
  submit,
} = useRolePermissionDialog()

defineExpose({ open })
</script>

<style scoped>
.root-notice {
  margin-bottom: 16px;
}
.perm-dialog :deep(.el-dialog) {
  background: #0f172a;
  border-radius: 10px;
}

.info-card,
.perm-card {
  margin-bottom: 12px;
  background: #111827 !important;
  border: 1px solid #1f2937 !important;
  color: #e5e7eb;
}

.tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 12px 0;
}

.perm-group {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.perm-item {
  height: auto;
  padding: 6px 10px;
  background: rgba(255, 255, 255, 0.04);
  border-radius: 6px;
}
.perm-label {
  display: flex;
  flex-direction: column;
  gap: 3px;
  line-height: 1.5;
}
.perm-label small {
  color: #8d9db4;
  font:
    11px/1.5 Consolas,
    monospace;
}

:deep(.el-checkbox__label) {
  color: #e5e7eb;
}

.title {
  font-weight: bold;
  margin-bottom: 10px;
}

.row {
  color: #9ca3af;
  font-size: 13px;
}
</style>
