<template>
  <div class="page">
    <div class="header">
      <h1>标签管理</h1>
      <p class="subtitle">管理系统标签信息</p>
    </div>

    <div class="content">
      <!-- 搜索 -->
      <el-form inline class="filter-form">
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
import { ref, reactive, onMounted, watch } from 'vue'
import { debounce } from 'lodash-es'
import { ElMessage, ElMessageBox } from 'element-plus'

import BaseTable from '@/components/basic/BaseTable.vue'
import { useTable } from '@/composables/useTable'

import type { TagVO, CreateTagDTO, UpdateTagDTO } from '@/api/tag'
import { pageTagApi, createTagApi, updateTagApi, deleteTagApi } from '@/api/tag'

/**
 * 搜索
 */
const nameFilter = ref('')

/**
 * 弹窗
 */
const dialogVisible = ref(false)
const dialogTitle = ref('')

/**
 * 表单
 */
const tagForm = reactive<Partial<TagVO & CreateTagDTO>>({
  id: undefined,
  name: '',
  description: '',
})

/**
 * 表格
 */
const {
  list: tags,
  loading,
  current,
  size,
  total,
  fetch,
  resetPage,
} = useTable<TagVO, Parameters<typeof pageTagApi>>(pageTagApi)

/**
 * 列定义
 */
const columns = [
  { label: 'ID', prop: 'id', width: 80 },
  { label: '名称', prop: 'name' },
  { label: '描述', prop: 'description' },
  { label: '创建时间', prop: 'createTime', width: 180 },
  { label: '操作', slot: 'actions', width: 160 },
]

/**
 * 查询
 */
const fetchData = async () => {
  await fetch(nameFilter.value, current.value, size.value)
}

const debouncedFetch = debounce(() => {
  resetPage()
  fetchData()
}, 300)

watch(nameFilter, () => {
  debouncedFetch()
})

const onSearch = () => {
  resetPage()
  fetchData()
}

/**
 * 分页
 */
const onPageChange = (page: number) => {
  fetch(nameFilter.value, page, size.value)
}

const onSizeChange = (newSize: number) => {
  fetch(nameFilter.value, current.value, newSize)
}

/**
 * 新增
 */
const openCreate = () => {
  dialogTitle.value = '新增标签'
  Object.assign(tagForm, {
    id: undefined,
    name: '',
    description: '',
  })
  dialogVisible.value = true
}

/**
 * 编辑
 */
const openEdit = (row: TagVO) => {
  dialogTitle.value = '编辑标签'
  Object.assign(tagForm, row)
  dialogVisible.value = true
}

/**
 * 保存
 */
const saveTag = async () => {
  if (!tagForm.name?.trim()) {
    ElMessage.warning('请输入标签名称')
    return
  }

  let res

  if (tagForm.id) {
    const dto: UpdateTagDTO = {
      id: tagForm.id,
      name: tagForm.name!,
      description: tagForm.description,
    }
    res = await updateTagApi(dto)
  } else {
    const dto: CreateTagDTO = {
      name: tagForm.name!,
      description: tagForm.description,
    }
    res = await createTagApi(dto)
  }

  if (res.code === 200) {
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchData()
  } else {
    ElMessage.error(res.message || '操作失败')
  }
}

/**
 * 删除
 */
const deleteTag = async (row: TagVO) => {
  await ElMessageBox.confirm(`确认删除标签【${row.name}】吗？`, '提示', { type: 'warning' })
  const res = await deleteTagApi(row.id)
  if (res.code === 200) {
    ElMessage.success('删除成功')
    fetchData()
  } else {
    ElMessage.error(res.message || '删除失败')
  }
}

/**
 * 初始化
 */
onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  min-height: 85vh;
}

.header {
  text-align: center;
  margin-bottom: 24px;
}

.header h1 {
  font-size: 34px;
  font-weight: bold;
  background: linear-gradient(90deg, #38bdf8, #818cf8);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.subtitle {
  color: #94a3b8;
}

.content {
  flex: 1;
}

.filter-form {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}
</style>
