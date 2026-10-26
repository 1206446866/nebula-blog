import {
  type ArticleVO,
  changeArticleStatusApi,
  deleteArticleApi,
  getArticlePageApi,
} from '@/api/article'
import type { TableColumn } from '@/components/basic/table'
import { useTable } from '@/composables/useTable'
import { useAuthStore } from '@/stores/auth'
import { formatDateTime } from '@/utils/date'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ref } from 'vue'
import { useRouter } from 'vue-router'

/** 处理文章管理页面的状态与业务交互。 */
export function useArticleManager() {
  const titleFilter = ref('')
  const auth = useAuthStore()
  const router = useRouter()
  const {
    list: articles,
    loading,
    error: tableError,
    current,
    size,
    total,
    reload: reloadTable,
    search: onSearch,
    changePage: onPageChange,
    changeSize: onSizeChange,
  } = useTable<ArticleVO>(
    ({ current, size, signal }) =>
      getArticlePageApi({ title: titleFilter.value, current, size }, signal),
    { watch: [titleFilter] },
  )
  const columns: TableColumn<ArticleVO>[] = [
    { label: 'ID', prop: 'id', width: 80 },
    { label: '标题', prop: 'title' },
    { label: '作者', prop: 'author', width: 140 },
    { label: '状态', slot: 'status', width: 120 },
    {
      label: '创建时间',
      prop: 'createTime',
      width: 180,
      formatter: (row: ArticleVO) => formatDateTime(row.createTime) || '—',
    },
    { label: '操作', slot: 'actions', width: 300 },
  ]
  const openCreate = () => router.push({ name: 'ArticleCreate', query: { from: 'admin' } })
  const openEdit = (row: ArticleVO, preview = false) =>
    router.push({
      name: 'ArticleEdit',
      params: { id: row.id },
      query: { from: 'admin', ...(preview ? { mode: 'preview' } : {}) },
    })

  // 删除文章
  const deleteArticle = async (row: ArticleVO) => {
    const confirmed = await ElMessageBox.confirm(
      `确定删除${row.status === 1 ? '文章' : '草稿'}【${row.title}】吗？`,
      '删除确认',
      { type: 'warning' },
    ).then(
      () => true,
      () => false,
    )
    if (!confirmed) return
    const res = await deleteArticleApi(row.id!)
    if (res.code === 200 && res.data) {
      ElMessage.success('删除成功')

      await reloadTable()
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
      row.scheduledPublishAt = null
      row.scheduleError = null
      ElMessage.success('状态修改成功')
    } else {
      ElMessage.error('状态修改失败')
    }
  }

  return {
    titleFilter,
    auth,
    articles,
    loading,
    tableError,
    current,
    size,
    total,
    reloadTable,
    onSearch,
    onPageChange,
    onSizeChange,
    columns,
    openCreate,
    openEdit,
    deleteArticle,
    switchStatus,
  }
}
