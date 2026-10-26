import { type CommentVO, deleteCommentByIdApi, getCommentRepliesApi } from '@/api/comment'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { EmitFn } from 'vue'
import { onBeforeUnmount, ref, watch } from 'vue'

/** 管理评论回复与删除的状态与交互。 */
export function useCommentThread(
  props: Readonly<{ comment: CommentVO; articleId: number; authorId?: number }>,
  emit: EmitFn<{ removed: [] }>,
) {
  const expanded = ref(false)
  const loading = ref(false)
  const error = ref(false)
  const deleting = ref(false)
  const submitting = ref(false)
  const target = ref<CommentVO | null>(null)
  const replies = ref<CommentVO[]>([])
  const replyCount = ref(props.comment.replyCount || 0)
  const replyPage = ref(1)
  const replySize = 5
  let requestId = 0

  watch(
    () => props.comment.replyCount,
    (value) => {
      replyCount.value = value || 0
    },
  )

  onBeforeUnmount(() => {
    requestId++
  })
  const loadReplies = async (page: number) => {
    const id = ++requestId
    loading.value = true
    error.value = false
    try {
      const res = await getCommentRepliesApi(props.comment.id, page, replySize)
      if (id !== requestId) return
      const last = Math.max(1, Math.ceil(res.data.totalRow / replySize))
      if (page > last) {
        await loadReplies(last)
        return
      }
      replies.value = res.data.records
      replyCount.value = res.data.totalRow
      replyPage.value = res.data.pageNumber
    } catch {
      if (id === requestId) error.value = true
    } finally {
      if (id === requestId) loading.value = false
    }
  }
  const toggleExpanded = async () => {
    expanded.value = !expanded.value
    if (expanded.value) await loadReplies(replyPage.value)
  }
  const changeReplyPage = async (page: number) => {
    target.value = null
    await loadReplies(page)
  }
  const startReply = (comment: CommentVO) => {
    if (submitting.value || deleting.value) return
    target.value = comment
  }
  const replySent = async () => {
    target.value = null
    replyCount.value++
    expanded.value = true
    await loadReplies(Math.ceil(replyCount.value / replySize))
  }
  const remove = async (comment: CommentVO) => {
    if (deleting.value || submitting.value) return
    const confirmed = await ElMessageBox.confirm(
      '这条评论及其下的所有回复将一起删除，确定继续吗？',
      '删除评论',
      { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '保留' },
    ).then(
      () => true,
      () => false,
    )
    if (!confirmed) return
    deleting.value = true
    try {
      const res = await deleteCommentByIdApi(comment.id)
      if (!res.data) return
      target.value = null
      ElMessage.success('评论已删除')
      if (comment.id === props.comment.id) emit('removed')
      else await loadReplies(replyPage.value)
    } catch {
      /* 错误提示由请求层统一显示。 */
    } finally {
      deleting.value = false
    }
  }

  return {
    expanded,
    loading,
    error,
    deleting,
    submitting,
    target,
    replies,
    replyCount,
    replyPage,
    replySize,
    loadReplies,
    toggleExpanded,
    changeReplyPage,
    startReply,
    replySent,
    remove,
  }
}
