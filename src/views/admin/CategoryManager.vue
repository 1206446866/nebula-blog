<template>
  <div class="page">
    <div class="header">
      <h1>分类管理</h1>
      <p class="subtitle">管理系统文章分类</p>
    </div>

    <div class="content">
      <!-- 搜索栏 -->
      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="请输入分类名称"
          clearable
          style="width: 240px"
          @keyup.enter="handleSearch"
        />

        <div class="toolbar-right">
          <el-button @click="handleSearch"> 搜索 </el-button>

          <el-button type="primary" @click="handleCreate"> 新增分类 </el-button>
        </div>
      </div>

      <!-- 表格 -->
      <BaseTable
        :data="categoryList"
        :columns="columns"
        :loading="loading"
        :current="current"
        :size="size"
        :total="total"
        @page-change="handlePageChange"
        @size-change="handleSizeChange"
      >
        <template #action="{ row }">
          <el-button type="primary" link @click="handleEdit(row)"> 编辑 </el-button>
          <el-button type="danger" link @click="handleDelete(row)"> 删除 </el-button>
        </template>
      </BaseTable>
    </div>
  </div>

  <el-dialog v-model="dialogVisible" title="新增分类" width="500px">
    <el-form :model="createForm" label-width="80px">
      <el-form-item label="分类名称">
        <el-input v-model="createForm.name" placeholder="请输入分类名称" />
      </el-form-item>

      <el-form-item label="分类描述">
        <el-input
          v-model="createForm.description"
          type="textarea"
          :rows="3"
          placeholder="请输入分类描述"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="dialogVisible = false"> 取消 </el-button>

      <el-button type="primary" @click="handleSubmit"> 确定 </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import BaseTable from '@/components/basic/BaseTable.vue'

import {
  type CategoryVO,
  createCategoryApi,
  type CreateCategoryDTO,
  deleteCategoryApi,
  pageCategoryApi,
  updateCategoryApi,
} from '@/api/category'
import { useTable } from '@/composables/useTable.ts'
import { debounce } from 'lodash-es'

const keyword = ref('')
const columns = [
  {
    prop: 'id',
    label: 'ID',
    width: 80,
  },
  {
    prop: 'name',
    label: '分类名称',
  },
  {
    prop: 'description',
    label: '分类描述',
  },
  {
    prop: 'createTime',
    label: '创建时间',
    width: 200,
  },
  {
    label: '操作',
    width: 120,
    slot: 'action',
  },
]
const dialogVisible = ref(false)
const createForm = ref<CreateCategoryDTO>({
  name: '',
  description: '',
})
const {
  list: categoryList,
  loading,
  current,
  size,
  total,
  fetch: pageCategory,
  resetPage,
} = useTable<CategoryVO, Parameters<typeof pageCategoryApi>>(pageCategoryApi)

const debouncedFetch = debounce(async () => {
  resetPage()
  await loadData()
}, 300)

watch(keyword, () => {
  debouncedFetch()
})
/**
 * 加载数据
 */
const loadData = async () => {
  loading.value = true
  await pageCategory({
    name: keyword.value,
    currentPage: current.value,
    size: size.value,
  })
}

/**
 * 搜索
 */
const handleSearch = () => {
  resetPage()
  loadData()
}

/**
 * 分页切换
 */
const handlePageChange = (page: number) => {
  current.value = page
  loadData()
}

/**
 * 页大小切换
 */
const handleSizeChange = (newSize: number) => {
  size.value = newSize
  resetPage()
  loadData()
}

/**
 * 新增分类
 */
const handleCreate = () => {
  createForm.value = {
    name: '',
    description: '',
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const res = await createCategoryApi(createForm.value)
  if (res.code === 200) {
    ElMessage.success('新增成功')
    dialogVisible.value = false
    await loadData()
  } else {
    ElMessage.error(res.message)
  }
}
/**
 * 编辑分类
 */
const handleEdit = async (row: CategoryVO) => {
  const res = await updateCategoryApi({
    id: row.id,
    name: row.name,
    description: row.description,
  })
  if (res.code === 200) {
    ElMessage.success('改动成功')
  }
}

const handleDelete = async (row: CategoryVO) => {
  try {
    await ElMessageBox.confirm(`确认删除分类【${row.name}】吗？`, '删除确认', {
      type: 'warning',
    })
    const res = await deleteCategoryApi(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      await loadData()
    } else {
      ElMessage.error(res.message)
    }
  } catch {
    //
  }
}
onMounted(() => {
  loadData()
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

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;

  margin-bottom: 16px;
}

.toolbar-right {
  display: flex;
  gap: 12px;
}
</style>
