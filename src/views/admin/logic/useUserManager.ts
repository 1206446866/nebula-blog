import { resetUserPasswordApi } from '@/api/auth'
import { getAllRolesApi, getRolesByUserIdApi, type RoleVO } from '@/api/role.ts'
import {
  deleteUserByIdApi,
  editUserApi,
  getUsersApi,
  switchStatusByIdApi,
  type EditUserDTO,
  type UserVO,
} from '@/api/user'
import type { TableColumn } from '@/components/basic/table'
import { useTable } from '@/composables/useTable'
import { Role } from '@/constants/role'
import { useAuthStore } from '@/stores/auth'
import { getCurrentUserId } from '@/utils/auth'
import { formatDateTime } from '@/utils/date'
import { isValidNewPassword, newPasswordMessage } from '@/utils/password-policy'
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'

/** 处理用户管理页面的状态与业务交互。 */
export function useUserManager() {
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
    if (!isValidNewPassword(password)) {
      ElMessage.warning(newPasswordMessage)
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
      // 请求拦截器统一显示可公开的服务端提示。
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
  const columns: TableColumn<UserVO>[] = [
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
    error: tableError,
    current,
    size,
    total,
    reload: reloadTable,
    search: onSearch,
    changePage: onPageChange,
    changeSize: onSizeChange,
  } = useTable<UserVO>(
    ({ current, size, signal }) =>
      getUsersApi(roleFilter.value, userNameFilter.value, current, size, signal),
    { watch: [roleFilter, userNameFilter] },
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
      await reloadTable()
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
      await reloadTable()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  }

  onMounted(async () => {
    try {
      const res = await getAllRolesApi()
      roleOptions.value = res.data || []
    } catch {
      /* 请求拦截器统一显示错误，保留现有角色选项。 */
    }
  })

  return {
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
  }
}
