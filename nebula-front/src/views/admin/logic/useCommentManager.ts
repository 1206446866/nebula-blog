import { type CommentVO, deleteCommentByIdApi, getCommentPageApi } from '@/api/comment'
import type { TableColumn } from '@/components/basic/table'
import { useTable } from '@/composables/useTable'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ref } from 'vue'

/** 处理评论管理页面的状态与业务交互。 */
export function useCommentManager() {
  const articleIdFilter = ref<number>()
  const contentFilter = ref('')
  const columns: TableColumn<CommentVO>[] = [
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
    error: tableError,
    current,
    size,
    total,
    reload: reloadTable,
    search: onSearch,
    changePage: onPageChange,
    changeSize: onSizeChange,
  } = useTable<CommentVO>(
    ({ current, size, signal }) =>
      getCommentPageApi(articleIdFilter.value, contentFilter.value, current, size, signal),
    { watch: [articleIdFilter, contentFilter] },
  )
  const deleteComment = async (row: CommentVO) => {
    const confirmed = await ElMessageBox.confirm(
      `确定删除评论【${row.id}】及其下的所有回复吗？`,
      '删除确认',
      {
        type: 'warning',
      },
    ).then(
      () => true,
      () => false,
    )
    if (!confirmed) return

    const res = await deleteCommentByIdApi(row.id)

    if (res.code === 200 && res.data) {
      ElMessage.success('删除成功')

      await reloadTable()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  }

  return {
    articleIdFilter,
    contentFilter,
    columns,
    comments,
    loading,
    tableError,
    current,
    size,
    total,
    reloadTable,
    onSearch,
    onPageChange,
    onSizeChange,
    deleteComment,
  }
}
