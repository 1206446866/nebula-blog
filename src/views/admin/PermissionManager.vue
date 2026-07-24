<template>
  <div class="page">
    <div class="header">
      <h1>权限管理</h1>
      <p class="subtitle">管理系统权限与功能点</p>
    </div>

    <div class="content">
      <!-- 查询区域 -->
      <el-form inline class="filter-form">
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
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑权限' : '新增权限'" width="400px">
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
import { onMounted, reactive, ref, watch } from 'vue'
import {
  createPermissionApi,
  type CreatePermissionDto,
  getPermissionPage,
  type PermissionVO,
  removePermissionApi,
  updatePermissionApi,
  type UpdatePermissionDto,
} from '@/api/permission.ts'
import { useTable } from '@/composables/useTable.ts'
import BaseTable from '@/components/basic/BaseTable.vue'
import SafeButton from '@/components/basic/SafeButton.vue'
import { debounce } from 'lodash-es'
import { ElMessage, ElMessageBox } from 'element-plus'

const nameFilter = ref('')
const dialogVisible = ref(false)

const form = reactive<Partial<PermissionVO>>({
  id: undefined,
  name: '',
  description: '',
})

const columns = [
  { prop: 'id', label: 'ID' },
  { prop: 'name', label: '权限编码' },
  { prop: 'description', label: '描述' },
  { prop: 'createTime', label: '创建时间' },
  { slot: 'actions', label: '操作', width: 160 },
]
const {
  list: permissions,
  loading,
  current,
  size,
  total,
  fetch,
  resetPage,
} = useTable<PermissionVO, Parameters<typeof getPermissionPage>>(getPermissionPage)
const onSearch = async () => {
  resetPage()
  await fetch({
    name: nameFilter.value,
    current: current.value,
    size: size.value,
  })
}

const onPageChange = async (page: number) => {
  await fetch({
    name: nameFilter.value,
    current: page,
    size: size.value,
  })
}

const onSizeChange = async (val: number) => {
  await fetch({
    name: nameFilter.value,
    current: current.value,
    size: val,
  })
}
const debounced = debounce(onSearch, 300)
watch(nameFilter, () => {
  debounced()
})

const openCreate = () => {
  Object.assign(form, { id: undefined, name: '', description: '' })
  dialogVisible.value = true
}

const openEdit = (row: PermissionVO) => {
  Object.assign(form, row)
  dialogVisible.value = true
}

const remove = async (row: PermissionVO) => {
  await ElMessageBox.confirm(`确定删除权限【${row.name}】吗？`, '提示', {
    type: 'warning',
  })
  await removePermissionApi(row.id)
  ElMessage.success('删除成功')
  await onSearch()
}

const submitForm = async () => {
  if (!form.name) {
    ElMessage.warning('请输入权限代号')
    return
  }
  if (form.id) {
    await updatePermissionApi(<UpdatePermissionDto>form)
    ElMessage.success('更新成功')
  } else {
    await createPermissionApi(<CreatePermissionDto>form)
    ElMessage.success('创建成功')
  }
  dialogVisible.value = false
  await onSearch()
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
