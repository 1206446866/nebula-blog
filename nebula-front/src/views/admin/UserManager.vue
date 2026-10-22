<template>
  <div class="page">
    <div class="header">
      <h1>用户管理</h1>
      <p class="subtitle">管理平台用户信息与角色权限</p>
    </div>

    <div class="content">
      <!-- 查询区域 -->
      <el-form inline class="filter-form">
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
    <el-dialog v-model="dialogVisible" title="编辑用户" width="min(400px, calc(100vw - 32px))">
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
            placeholder="8至64个字符"
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
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { debounce } from 'lodash-es'
import { formatDateTime } from '@/utils/date'
import { ElMessage, ElMessageBox } from 'element-plus'
import BaseTable from '@/components/basic/BaseTable.vue'
import SafeButton from '@/components/basic/SafeButton.vue'
import { useTable } from '@/composables/useTable'
import {
  deleteUserByIdApi,
  editUserApi,
  getUsersApi,
  switchStatusByIdApi,
  type UserVO,
  type EditUserDTO,
} from '@/api/user'
import { getCurrentUserId } from '@/utils/auth'
import { getAllRolesApi, getRolesByUserIdApi, type RoleVO } from '@/api/role.ts'
import { useAuthStore } from '@/stores/auth'
import { Role } from '@/constants/role'
import { resetUserPasswordApi } from '@/api/auth'

const authStore = useAuthStore()
const isSuperAdmin = computed(() => authStore.hasRole(Role.SUPER_ADMIN))
const originalRoleIds = ref<number[]>([])
const passwordResetVisible = ref(false)
const passwordResetting = ref(false)
const passwordResetTarget = ref<UserVO | null>(null)
const passwordResetForm = reactive({ operatorPassword: '', newPassword: '', confirmPassword: '' })
const canResetPassword = (row: UserVO) =>
  row.id !== Number(getCurrentUserId()) &&
  authStore.hasRole(Role.ADMIN) &&
  authStore.hasPermission('user:update') &&
  (isSuperAdmin.value ||
    !row.roleIds?.some(
      (id) =>
        id === 1 ||
        id === 2 ||
        ['ADMIN', 'ROLE_ADMIN', 'SUPER_ADMIN', 'ROLE_SUPER_ADMIN'].includes(
          roleOptions.value.find((role) => role.id === id)?.name || '',
        ),
    ))
const clearPasswordReset = () => {
  passwordResetForm.operatorPassword = ''
  passwordResetForm.newPassword = ''
  passwordResetForm.confirmPassword = ''
  passwordResetTarget.value = null
}
const openPasswordReset = (row: UserVO) => {
  if (!canResetPassword(row) || passwordResetting.value) return
  clearPasswordReset()
  passwordResetTarget.value = row
  passwordResetVisible.value = true
}
const submitPasswordReset = async () => {
  if (passwordResetting.value || !passwordResetTarget.value) return
  if (!passwordResetForm.operatorPassword.trim()) {
    ElMessage.warning('请输入你的当前密码')
    return
  }
  const password = passwordResetForm.newPassword
  if (
    password.length < 8 ||
    password.length > 64 ||
    new TextEncoder().encode(password).length > 72
  ) {
    ElMessage.warning('新密码需为8至64个字符且不超过72字节')
    return
  }
  if (password !== passwordResetForm.confirmPassword) {
    ElMessage.warning('两次新密码不一致')
    return
  }
  passwordResetting.value = true
  try {
    const result = await resetUserPasswordApi(passwordResetTarget.value.id, {
      ...passwordResetForm,
    })
    if (result.data === true) {
      passwordResetVisible.value = false
      clearPasswordReset()
      ElMessage.success('密码已重置，该用户原有登录会话已失效')
    }
  } catch {
    // The request interceptor displays safe server feedback.
  } finally {
    passwordResetting.value = false
  }
}

const roleFilter = ref('')
const userNameFilter = ref('')
const dialogVisible = ref(false)
const editForm = reactive<EditUserDTO>({
  id: 0,
  username: '',
  roleIds: [],
})
const columns = [
  {
    label: 'ID',
    prop: 'id',
    width: 80,
  },
  {
    label: '用户名',
    prop: 'username',
  },
  {
    label: '角色',
    slot: 'roles',
    width: 120,
  },
  {
    label: '状态',
    slot: 'status',
    width: 120,
  },
  {
    label: '创建时间',
    prop: 'createTime',
    width: 180,
    formatter: (row: UserVO) => formatDateTime(row.createTime) || '—',
  },
  {
    label: '操作',
    slot: 'actions',
    width: 240,
  },
]
const roleOptions = ref<RoleVO[]>([])
const {
  list: users,
  loading,
  current,
  size,
  total,
  fetch: fetchUsers,
  resetPage,
} = useTable<UserVO, Parameters<typeof getUsersApi>>(getUsersApi)

const onSearch = async () => {
  resetPage()
  await fetchUsers(roleFilter.value, userNameFilter.value, current.value, size.value)
}

const onPageChange = async (page: number) => {
  await fetchUsers(roleFilter.value, userNameFilter.value, page, size.value)
}

const onSizeChange = async (newSize: number) => {
  await fetchUsers(roleFilter.value, userNameFilter.value, current.value, newSize)
}

const debouncedFetch = debounce(async () => {
  resetPage()
  await onSearch()
}, 300)

watch(
  [roleFilter, userNameFilter],
  () => {
    debouncedFetch()
  },
  { flush: 'post' },
)

const switchStatus = async (row: UserVO) => {
  const targetStatus = row.status === 0 ? 1 : 0
  await switchStatusByIdApi(row.id, targetStatus)
  row.status = targetStatus
  ElMessage.success('状态修改成功')
}

const editUser = async (row: UserVO) => {
  if (!isSuperAdmin.value) return
  const res = await getRolesByUserIdApi(row.id)
  if (res.code === 200 && res.data) {
    editForm.id = row.id
    editForm.username = row.username
    editForm.roleIds = res.data.map((item) => item.id)
    originalRoleIds.value = [...editForm.roleIds]
  } else {
    ElMessage.error('拉取角色信息失败')
    return
  }
  dialogVisible.value = true
}

const saveEdit = async () => {
  if (!isSuperAdmin.value) return
  if (editForm.roleIds.includes(1) && !originalRoleIds.value.includes(1)) {
    const confirmed = await ElMessageBox.confirm(
      `将【${editForm.username}】提升为超级管理员后，该账号将拥有全部管理权限。是否继续？`,
      '提升超级管理员',
      { type: 'warning', confirmButtonText: '确认提升', cancelButtonText: '取消' },
    ).then(
      () => true,
      () => false,
    )
    if (!confirmed) return
  }
  const res = await editUserApi({
    id: editForm.id!,
    username: editForm.username!,
    roleIds: editForm.roleIds!,
  })
  if (res.code === 200 && res.data) {
    ElMessage.success('修改成功')
    dialogVisible.value = false
    await fetchUsers(roleFilter.value, userNameFilter.value, current.value, size.value)
  } else {
    ElMessage.error(res.message || '修改失败')
  }
}

const deleteUser = async (row: UserVO) => {
  await ElMessageBox.confirm(`确定删除用户【${row.username}】吗？`, '删除确认', {
    type: 'warning',
  })

  const res = await deleteUserByIdApi(row.id)
  if (res.code === 200 && res.data) {
    ElMessage.success('删除成功')
    await fetchUsers(roleFilter.value, userNameFilter.value, current.value, size.value)
  } else {
    ElMessage.error(res.message || '删除失败')
  }
}

onMounted(async () => {
  await fetchUsers(roleFilter.value, userNameFilter.value, 1, size.value)
  const res = await getAllRolesApi()
  roleOptions.value = res.data || []
})
</script>

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
.page {
  display: flex;
  flex-direction: column;
  min-height: 85vh; /* 确保撑满全屏 */
}

/* 表格和表单占据剩余空间 */
.content {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.status-action span {
  display: flex;
  justify-content: center;
  margin-left: 7%;
  margin-right: 7%;
}

.header {
  text-align: center;
  margin-bottom: 24px;
}

.header h1 {
  font-size: 36px;
  font-weight: bold;
  margin: 0;
  background: linear-gradient(90deg, #38bdf8, #818cf8);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.header .subtitle {
  color: #94a3b8;
  margin-top: 4px;
  font-size: 14px;
}

.filter-form {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}

.status-action el-tag {
  display: inline-block; /* 确保不撑开 */
  padding: 0 6px; /* 调整内边距，更像文本 */
  font-size: 12px;
  cursor: default; /* 避免鼠标悬停像可点击 */
  line-height: 20px;
}

:deep(.el-dialog) {
  background: #111827;
}

:deep(.el-dialog__title) {
  color: white;
}
</style>
