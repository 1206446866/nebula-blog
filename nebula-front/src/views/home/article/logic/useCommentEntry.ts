import { type CommentVO, likeCommentApi } from '@/api/comment'
import { useAuthStore } from '@/stores/auth'
import { avatarUrl as resourceAvatarUrl } from '@/utils/resource'
import type { EmitFn } from 'vue'
import { computed, ref, watch } from 'vue'

/** 管理评论展示与点赞的状态与交互。 */
export function useCommentEntry(
  props: Readonly<{
    comment: CommentVO
    authorId?: number
    reply?: boolean
    disabled?: boolean
  }>,
  emit: EmitFn<{ reply: [comment: CommentVO]; delete: [comment: CommentVO] }>,
) {
  const auth = useAuthStore()
  const displayName = computed(() => props.comment.username || '已注销用户')
  const avatarUrl = computed(() => resourceAvatarUrl(props.comment.avatar))
  const canDelete = computed(
    () =>
      auth.hasPermission('comment:delete') &&
      (auth.hasRole('ADMIN') || props.comment.userId === Number(auth.user?.id)),
  )
  const liked = ref(false)
  const count = ref(0)
  const liking = ref(false)

  watch(
    () => [props.comment.liked, props.comment.likeCount],
    () => {
      liked.value = !!props.comment.liked
      count.value = props.comment.likeCount || 0
    },
    { immediate: true },
  )
  const toggleLike = async () => {
    if (liking.value || props.disabled || !auth.hasPermission('comment:update')) return
    liking.value = true
    try {
      const res = await likeCommentApi({ commentId: props.comment.id })
      const next = !!res.data
      if (next !== liked.value) count.value = Math.max(0, count.value + (next ? 1 : -1))
      liked.value = next
    } catch {
      /* 错误提示由请求层统一显示。 */
    } finally {
      liking.value = false
    }
  }

  return { auth, displayName, avatarUrl, canDelete, liked, count, liking, toggleLike }
}
