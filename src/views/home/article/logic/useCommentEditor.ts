import { releaseCommentApi } from '@/api/comment'
import { useAuthStore } from '@/stores/auth'
import { ElMessage, type InputInstance } from 'element-plus'
import type { EmitFn } from 'vue'
import { computed, nextTick, onMounted, ref } from 'vue'

/** 管理评论输入与提交的状态与交互。 */
export function useCommentEditor(
  props: Readonly<{
    articleId: number
    parentId?: number
    replyTo?: string
    disabled?: boolean
  }>,
  emit: EmitFn<{ success: []; cancel: []; busy: [value: boolean] }>,
) {
  const auth = useAuthStore()
  const canWrite = computed(() => auth.hasPermission('comment:create'))
  const content = ref('')
  const submitting = ref(false)
  const input = ref<InputInstance>()

  onMounted(async () => {
    if (props.parentId) {
      await nextTick()
      input.value?.focus()
    }
  })
  const submit = async () => {
    if (props.disabled || submitting.value || !canWrite.value || !content.value.trim()) return
    submitting.value = true
    emit('busy', true)
    try {
      const res = await releaseCommentApi({
        articleId: props.articleId,
        parentId: props.parentId,
        content: content.value.trim(),
      })
      if (res.data) {
        content.value = ''
        ElMessage.success(props.parentId ? '回复已发送' : '评论已发表')
        emit('success')
      }
    } catch {
      /* 保留尚未提交的内容，由请求层统一显示错误。 */
    } finally {
      submitting.value = false
      emit('busy', false)
    }
  }

  return { canWrite, content, submitting, input, submit }
}
