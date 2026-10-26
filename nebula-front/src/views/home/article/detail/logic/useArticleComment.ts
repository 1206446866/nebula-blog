import { type CommentVO, getCommentThreadsApi } from '@/api/comment'
import { onBeforeUnmount, ref, watch } from 'vue'

/** 管理文章评论分页的状态与交互。 */
export function useArticleComment(props: Readonly<{ articleId: number; authorId?: number }>) {
  const list = ref<CommentVO[]>([])
  const page = ref(1)
  const pageSize = 10
  const total = ref(0)
  const loading = ref(false)
  const error = ref(false)
  let requestId = 0
  const loadComments = async () => {
    const id = ++requestId
    loading.value = true
    error.value = false
    try {
      const res = await getCommentThreadsApi(props.articleId, page.value, pageSize)
      if (id !== requestId) return
      const last = Math.max(1, Math.ceil(res.data.totalRow / pageSize))
      if (page.value > last) {
        page.value = last
        await loadComments()
        return
      }
      list.value = res.data.records
      page.value = res.data.pageNumber
      total.value = res.data.totalRow
    } catch {
      if (id === requestId) error.value = true
    } finally {
      if (id === requestId) loading.value = false
    }
  }
  const commentSent = async () => {
    page.value = 1
    await loadComments()
  }
  const handlePageChange = async (value: number) => {
    page.value = value
    await loadComments()
  }

  watch(
    () => props.articleId,
    () => {
      list.value = []
      page.value = 1
      total.value = 0
      void loadComments()
    },
    { immediate: true },
  )

  onBeforeUnmount(() => {
    requestId++
  })

  return {
    list,
    page,
    pageSize,
    total,
    loading,
    error,
    loadComments,
    commentSent,
    handlePageChange,
  }
}
