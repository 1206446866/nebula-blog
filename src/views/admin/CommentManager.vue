<template>
  <div class="page">
    <div class="header">
      <h1>评论管理</h1>
      <p class="subtitle">管理平台评论内容与互动信息</p>
    </div>

    <div class="content">
      <!-- 查询区域 -->
      <el-form inline class="filter-form">
        <el-form-item>
          <el-input v-model.number="articleIdFilter" placeholder="请输入文章ID" clearable />
        </el-form-item>
        <el-form-item>
          <el-input v-model="contentFilter" placeholder="请输入评论内容" clearable />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="onSearch"> 搜索 </el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <BaseTable
        :data="comments"
        :columns="columns"
        :loading="loading"
        :current="current"
        :size="size"
        :total="total"
        @page-change="onPageChange"
        @size-change="onSizeChange"
      >
        <template #content="{ row }">
          <el-tooltip :content="row.content" placement="top">
            <span class="ellipsis">
              {{ row.content }}
            </span>
          </el-tooltip>
        </template>

        <template #createTime="{ row }">
          {{ formatDate(row.createTime) }}
        </template>

        <template #actions="{ row }">
          <SafeButton size="small" type="danger" :onClick="() => deleteComment(row)">
            删除
          </SafeButton>
        </template>
      </BaseTable>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import BaseTable from '@/components/basic/BaseTable.vue'
import SafeButton from '@/components/basic/SafeButton.vue'
import { useTable } from '@/composables/useTable'

import { getCommentPageApi, deleteCommentByIdApi, type CommentVO } from '@/api/comment'
import { debounce } from 'lodash-es'
import { formatDate } from '@/utils/date.ts'
const articleIdFilter = ref<number>()
const contentFilter = ref('')
const columns = [
  {
    label: 'ID',
    prop: 'id',
    width: 80,
  },
  {
    label: '用户',
    prop: 'username',
    width: 140,
  },
  {
    label: '文章ID',
    prop: 'articleId',
    width: 120,
  },
  {
    label: '评论内容',
    slot: 'content',
  },
  {
    label: '创建时间',
    slot: 'createTime',
    width: 180,
  },
  {
    label: '操作',
    slot: 'actions',
    width: 120,
  },
]

const {
  list: comments,
  loading,
  current,
  size,
  total,
  fetch: fetchComments,
  resetPage,
} = useTable<CommentVO, Parameters<typeof getCommentPageApi>>(getCommentPageApi)

const debouncedFetch = debounce(async () => {
  resetPage()
  await onSearch()
}, 300)

watch([articleIdFilter, contentFilter], () => {
  debouncedFetch()
})

const loadComments = async (page = current.value, pageSize = size.value) => {
  await fetchComments(articleIdFilter.value, contentFilter.value, page, pageSize)
}

const onSearch = async () => {
  resetPage()
  await loadComments(1, size.value)
}

const onPageChange = async (page: number) => {
  await loadComments(page, size.value)
}

const onSizeChange = async (newSize: number) => {
  await loadComments(current.value, newSize)
}

const deleteComment = async (row: CommentVO) => {
  await ElMessageBox.confirm(`确定删除评论【${row.id}】吗？`, '删除确认', {
    type: 'warning',
  })

  const res = await deleteCommentByIdApi(row.id)

  if (res.code === 200 && res.data) {
    ElMessage.success('删除成功')

    await loadComments()
  } else {
    ElMessage.error(res.msg || '删除失败')
  }
}

onMounted(async () => {
  await loadComments()
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

.ellipsis {
  display: inline-block;
  max-width: 500px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
