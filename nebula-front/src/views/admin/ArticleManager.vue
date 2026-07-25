<template>
  <div class="page">
    <div class="header">
      <h1>文章管理</h1>
      <p class="subtitle">管理平台文章内容与发布信息</p>
    </div>

    <div class="content">
      <!-- 搜索 -->
      <el-form inline class="filter-form">
        <el-form-item label="标题">
          <el-input v-model="titleFilter" placeholder="输入标题搜索" clearable />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="onSearch"> 查询 </el-button>
        </el-form-item>

        <el-form-item>
          <el-button type="success" @click="openCreate"> 新增文章 </el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <BaseTable
        :data="articles"
        :columns="columns"
        :loading="loading"
        :current="current"
        :size="size"
        :total="total"
        @page-change="onPageChange"
        @size-change="onSizeChange"
      >
        <template #status="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'warning'">
            {{ row.status === 1 ? '已发布' : '草稿' }}
          </el-tag>
        </template>

        <template #actions="{ row }">
          <el-button size="small" type="primary" @click="openEdit(row)">编辑</el-button>
          <SafeButton
            size="small"
            :type="row.status === 1 ? 'warning' : 'success'"
            :onClick="() => switchStatus(row)"
          >
            {{ row.status === 1 ? '撤回' : '发布' }}
          </SafeButton>
          <SafeButton size="small" type="danger" :onClick="() => deleteArticle(row)">
            删除
          </SafeButton>
        </template>
      </BaseTable>

      <!-- 新增/编辑 Dialog -->
      <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px">
        <el-form :model="articleForm">
          <el-form-item label="标题">
            <el-input v-model="articleForm.title" />
          </el-form-item>

          <el-form-item label="分类">
            <el-select
              v-model="articleForm.categoryId"
              placeholder="-- 请选择分类 --"
              clearable
              style="width: 100%"
            >
              <el-option
                v-for="item in categories"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="标签">
            <el-select
              v-model="articleForm.tagIds"
              multiple
              collapse-tags
              collapse-tags-tooltip
              placeholder="--请选择标签--"
              style="width: 100%"
            >
              <el-option v-for="item in tags" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>

          <el-form-item label="内容">
            <div class="editor-container">
              <el-input v-model="articleForm.content" type="textarea" :rows="20" />
              <div class="markdown-body preview" v-html="md.render(articleForm.content)" />
            </div>
          </el-form-item>
        </el-form>

        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <SafeButton type="primary" :onClick="saveArticle">保存</SafeButton>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { debounce } from 'lodash-es'
import MarkdownIt from 'markdown-it'
import SafeButton from '@/components/basic/SafeButton.vue'
import BaseTable from '@/components/basic/BaseTable.vue'
import { useTable } from '@/composables/useTable'
import {
  type ArticleVO,
  changeArticleStatusApi,
  createArticleApi,
  deleteArticleApi,
  getArticleByIdApi,
  getArticlePageApi,
  updateArticleApi,
} from '@/api/article'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listTagApi, type TagVO } from '@/api/tag.ts'
import { type CategoryVO, listCategoryApi } from '@/api/category.ts'

const md = new MarkdownIt()
const titleFilter = ref('')
const dialogVisible = ref(false)
const dialogTitle = ref('')
const categories = ref<CategoryVO[]>([])
const tags = ref<TagVO[]>([])
const articleForm = reactive({
  viewCount: 0,
  title: '',
  content: '',
  author: '',
  createTime: '',
  id: 0,
  status: 0,
  updateTime: '',
  categoryId: undefined as number | undefined,
  tagIds: [] as number[],
})

// 使用 useTable 管理表格数据与分页
const {
  list: articles,
  loading,
  current,
  size,
  total,
  fetch: fetchArticles,
  resetPage,
} = useTable<ArticleVO, Parameters<typeof getArticlePageApi>>(getArticlePageApi)

const columns = [
  { label: 'ID', prop: 'id', width: 80 },
  { label: '标题', prop: 'title' },
  { label: '作者', prop: 'author', width: 140 },
  { label: '状态', slot: 'status', width: 120 },
  { label: '创建时间', prop: 'createTime', width: 180 },
  { label: '操作', slot: 'actions', width: 220 },
]

const loadOptions = async () => {
  const [categoryRes, tagRes] = await Promise.all([listCategoryApi(), listTagApi()])
  if (categoryRes.code === 200) {
    categories.value = categoryRes.data || []
  }
  if (tagRes.code === 200) {
    tags.value = tagRes.data || []
  }
}

// 搜索
const debouncedFetch = debounce(() => {
  resetPage()
  fetchArticles({ title: titleFilter.value, current: current.value, size: size.value })
}, 300)

watch(titleFilter, () => {
  debouncedFetch()
})

const onSearch = () => {
  resetPage()
  fetchArticles({ title: titleFilter.value, current: current.value, size: size.value })
}

// 分页事件
const onPageChange = (page: number) => {
  fetchArticles({ title: titleFilter.value, current: page, size: size.value })
}

const onSizeChange = (newSize: number) => {
  fetchArticles({ title: titleFilter.value, current: current.value, size: newSize })
}

// 新增 / 编辑
const openCreate = () => {
  dialogTitle.value = '新增文章'
  Object.assign(articleForm, {
    id: undefined,
    title: '',
    content: '',
    categoryId: undefined,
    tagIds: [],
  })
  dialogVisible.value = true
}

const openEdit = async (row: ArticleVO) => {
  dialogTitle.value = '编辑文章'
  const res = await getArticleByIdApi(row.id)
  if (res.code === 200 && res.data) {
    Object.assign(articleForm, res.data)
    dialogVisible.value = true
  }
}

const saveArticle = async () => {
  if (!articleForm.title?.trim()) {
    ElMessage.warning('请输入标题')
    return
  }

  if (!articleForm.categoryId) {
    ElMessage.warning('请选择分类')
    return
  }

  if (!articleForm.content?.trim()) {
    ElMessage.warning('请输入内容')
    return
  }

  let res
  if (articleForm.id) {
    res = await updateArticleApi({
      id: articleForm.id,
      title: articleForm.title,
      content: articleForm.content,
      categoryId: articleForm.categoryId,
      tagIds: articleForm.tagIds,
    })
  } else {
    res = await createArticleApi({
      title: articleForm.title,
      content: articleForm.content,
      categoryId: articleForm.categoryId,
      tagIds: articleForm.tagIds,
    })
  }

  if (res.code === 200 && res.data) {
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await fetchArticles({ title: titleFilter.value, current: current.value, size: size.value })
  } else {
    ElMessage.error(res.msg || '保存失败')
  }
}

// 删除文章
const deleteArticle = async (row: ArticleVO) => {
  await ElMessageBox.confirm(`确定删除文章【${row.title}】吗？`, '删除确认', { type: 'warning' })
  const res = await deleteArticleApi(row.id!)
  if (res.code === 200 && res.data) {
    ElMessage.success('删除成功')
    await fetchArticles({ title: titleFilter.value, current: current.value, size: size.value })
  } else {
    ElMessage.error(res.msg || '删除失败')
  }
}

// 切换状态
const switchStatus = async (row: ArticleVO) => {
  const targetStatus = row.status === 1 ? 0 : 1
  const res = await changeArticleStatusApi({ id: row.id!, status: targetStatus })
  if (res.code === 200 && res.data) {
    row.status = targetStatus
    ElMessage.success('状态修改成功')
  } else {
    ElMessage.error('状态修改失败')
  }
}

// 初始加载
onMounted(async () => {
  await loadOptions()
  await fetchArticles({ title: titleFilter.value, current: current.value, size: size.value })
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

.subtitle {
  color: #94a3b8;
  margin-top: 4px;
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

:deep(.el-form-item__label) {
  color: #e5e7eb;
}

.editor-container {
  display: flex;
  gap: 16px;
}

.editor-container > * {
  flex: 1;
}
</style>
