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
          <el-input v-model="roleFilter" placeholder="输入权限筛选" clearable />
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

          <template v-else>
            <el-button size="small" type="primary" @click="editUser(row)"> 编辑 </el-button>

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
    <el-dialog v-model="dialogVisible" title="编辑用户" width="400px">
      <el-form :model="editForm">
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
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, reactive } from 'vue'
import { debounce } from 'lodash-es'
import { ElMessage, ElMessageBox } from 'element-plus'
import BaseTable from '@/components/basic/BaseTable.vue'
import SafeButton from '@/components/basic/SafeButton.vue'
import { useTable } from '@/composables/useTable'
import {
  getUsersApi,
  deleteUserByIdApi,
  switchStatusByIdApi,
  type UserVO,
  editUserApi,
} from '@/api/user'
import { getCurrentUserId } from '@/utils/auth'
import { getAllRolesApi, getRolesByUserIdApi, type RoleVO } from '@/api/role.ts'

const roleFilter = ref('')
const userNameFilter = ref('')
const dialogVisible = ref(false)
const editForm = reactive<UserVO>({
  avatar: '',
  createTime: '',
  id: 0,
  username: '',
  roleIds: [],
  status: 0,
  nid: '',
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
  Object.assign(editForm, row)
  const res = await getRolesByUserIdApi(row.id)
  if (res.code === 200 && res.data) {
    editForm.roleIds = res.data.map((item) => item.id)
  } else {
    ElMessage.error('拉取角色信息失败')
  }
  dialogVisible.value = true
}

const saveEdit = async () => {
  const res = await editUserApi({
    id: editForm.id!,
    username: editForm.username!,
    roleIds: editForm.roleIds!,
  })
  if (res.code === 200 && res.data) {
    ElMessage.success('修改成功')
    dialogVisible.value = false
    await fetchUsers()
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
