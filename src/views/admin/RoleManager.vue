<template>
  <div class="page">
    <div class="header">
      <h1>角色管理</h1>
      <p class="subtitle">管理系统角色与权限分配</p>
    </div>

    <div class="content">
      <!-- 查询区域 -->
      <el-form inline class="filter-form">
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
        :current="current"
        :size="size"
        :total="total"
        @page-change="onPageChange"
        @size-change="onSizeChange"
      >
        <!-- 创建时间 -->
        <template #createTime="{ row }">
          {{ row.createTime }}
        </template>

        <!-- 操作 -->
        <template #actions="{ row }">
          <!-- 🔥 新增 -->
          <el-button size="small" type="warning" @click="openPermission(row)"> 分配权限 </el-button>

          <el-button size="small" type="primary" @click="openEdit(row)"> 编辑 </el-button>

          <SafeButton size="small" type="danger" :onClick="() => deleteRole(row)">
            删除
          </SafeButton>
        </template>
      </BaseTable>
    </div>

    <!-- 编辑/新增 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑角色' : '新增角色'" width="400px">
      <el-form :model="form">
        <el-form-item label="角色名称">
          <el-input v-model="form.name" />
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
    <RolePermissionDialog ref="permDialogRef" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { debounce } from 'lodash-es'
import { ElMessage, ElMessageBox } from 'element-plus'
import BaseTable from '@/components/basic/BaseTable.vue'
import SafeButton from '@/components/basic/SafeButton.vue'
import { useTable } from '@/composables/useTable'
import {
  getRolePageApi,
  createRole,
  updateRole,
  removeRole,
  type RoleVO,
  type UpdateRoleDto,
  type CreateRoleDto,
} from '@/api/role'
import RolePermissionDialog from '@/components/dialog/RolePermissionDialog.vue'

/** filter */
const descriptionFilter = ref('')

/** dialog */
const dialogVisible = ref(false)

/** form */
const form = reactive<Partial<RoleVO>>({
  id: undefined,
  name: '',
  description: '',
})

/** table columns（和UM风格一致） */
const columns = [
  { label: 'ID', prop: 'id', width: 80 },
  { label: '角色名称', prop: 'name' },
  { label: '描述', prop: 'description' },
  { label: '创建时间', slot: 'createTime', width: 180 },
  { label: '操作', slot: 'actions', width: 300 },
]

/** useTable */
const {
  list: roles,
  loading,
  current,
  size,
  total,
  fetch,
  resetPage,
} = useTable<RoleVO, Parameters<typeof getRolePageApi>>(getRolePageApi)

const permDialogRef = ref()

/** 搜索 */
const onSearch = async () => {
  resetPage()
  await fetch({
    description: descriptionFilter.value,
    current: current.value,
    size: size.value,
  })
}

/** 分页 */
const onPageChange = async (page: number) => {
  await fetch({
    description: descriptionFilter.value,
    current: page,
    size: size.value,
  })
}

const onSizeChange = async (newSize: number) => {
  await fetch({
    description: descriptionFilter.value,
    current: current.value,
    size: newSize,
  })
}

/** 防抖搜索 */
const debounced = debounce(onSearch, 300)
watch(descriptionFilter, () => {
  debounced()
})

/** 新增 */
const openCreate = () => {
  Object.assign(form, { id: undefined, name: '', description: '' })
  dialogVisible.value = true
}

/** 编辑 */
const openEdit = (row: RoleVO) => {
  Object.assign(form, row)
  dialogVisible.value = true
}

/** 提交 */
const submitForm = async () => {
  if (!form.name) {
    ElMessage.warning('请输入角色名称')
    return
  }
  if (form.id) {
    await updateRole(<UpdateRoleDto>form)
    ElMessage.success('更新成功')
  } else {
    await createRole(<CreateRoleDto>form)
    ElMessage.success('创建成功')
  }
  dialogVisible.value = false
  await onSearch()
}

/** 删除角色 */
const deleteRole = async (row: RoleVO) => {
  await ElMessageBox.confirm(`确定删除角色【${row.name}】吗？`, '提示', {
    type: 'warning',
  })
  await removeRole(row.id)
  ElMessage.success('删除成功')
  await onSearch()
}

const openPermission = async (row: RoleVO) => {
  permDialogRef.value.open(row)
}

onMounted(() => {
  onSearch()
})
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  min-height: 85vh;
}

.content {
  flex: 1;
  display: flex;
  flex-direction: column;
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
  font-size: 14px;
}

.filter-form {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}

:deep(.el-dialog) {
  background: #111827;
}

:deep(.el-dialog__title) {
  color: white;
}
</style>
