import type { ComputedRef, Ref } from 'vue'

/** 当前编辑页的生命周期，用于拒绝离开页面或切换文章后的旧响应。 */
export interface WriterRuntime {
  generation: number
  disposed: boolean
}

/** 正文历史和图片操作共享当前编辑会话，保持同一份正文与忙碌状态。 */
export interface WriterEditorContext {
  form: { content: string }
  textarea: Ref<HTMLTextAreaElement | undefined>
  saving: Ref<boolean>
  canEdit: ComputedRef<boolean>
  uploadingImages: Ref<boolean>
  libraryBusy: Ref<boolean>
  runtime: WriterRuntime
  isSessionCurrent: () => boolean
}
