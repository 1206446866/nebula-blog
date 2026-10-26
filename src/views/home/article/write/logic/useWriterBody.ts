import { insertMarkdownCode, setMarkdownHeading, type MarkdownEdit } from '@/utils/markdown-edit'
import { createTextHistory, type TextSnapshot } from '@/utils/text-history'
import { nextTick, ref, type Ref } from 'vue'
import type { WriterEditorContext } from './types'

/** 统一正文输入、格式工具和图片插入的撤销历史，并恢复光标与选区。 */
export function useWriterBody(
  context: WriterEditorContext & { loading: Ref<boolean>; loadError: Ref<boolean> },
) {
  const {
    form,
    textarea,
    saving,
    canEdit,
    uploadingImages,
    libraryBusy,
    runtime,
    isSessionCurrent,
    loading,
    loadError,
  } = context
  const bodyHistory = createTextHistory()
  const canUndoBody = ref(false)
  const canRedoBody = ref(false)
  let pendingBodyInput: TextSnapshot | undefined
  let compositionBefore: TextSnapshot | undefined
  let bodyRestoreRevision = 0

  async function insertMarkdown(prefix: string, suffix = '', placeholder = '文字') {
    const input = textarea.value
    if (!input || saving.value || !canEdit.value) return
    const start = input.selectionStart
    const end = input.selectionEnd
    const selection = form.content.slice(start, end) || placeholder
    await applyEdit({
      content: form.content.slice(0, start) + prefix + selection + suffix + form.content.slice(end),
      selectionStart: start + prefix.length,
      selectionEnd: start + prefix.length + selection.length,
    })
  }

  async function applyEdit(edit: MarkdownEdit) {
    if (runtime.disposed || saving.value || !canEdit.value || !isSessionCurrent()) return
    bodyHistory.record(bodySnapshot(), edit)
    updateBodyHistoryState()
    await restoreBody(edit)
  }

  function bodySnapshot(): TextSnapshot {
    return {
      content: form.content,
      selectionStart: textarea.value?.selectionStart ?? form.content.length,
      selectionEnd: textarea.value?.selectionEnd ?? form.content.length,
      selectionDirection: textarea.value?.selectionDirection ?? 'none',
    }
  }

  function updateBodyHistoryState() {
    canUndoBody.value = bodyHistory.canUndo
    canRedoBody.value = bodyHistory.canRedo
  }

  function resetBodyHistory() {
    pendingBodyInput = undefined
    compositionBefore = undefined
    bodyHistory.reset({ content: form.content, selectionStart: 0, selectionEnd: 0 })
    updateBodyHistoryState()
  }

  async function restoreBody(state: TextSnapshot) {
    const revision = ++bodyRestoreRevision
    form.content = state.content
    // 同步正文DOM，确保连续快捷键在Vue下一次渲染之前也能读取正确选区。
    const input = textarea.value
    if (input) {
      input.value = state.content
      input.focus()
      input.setSelectionRange(state.selectionStart, state.selectionEnd, state.selectionDirection)
    }
    const run = runtime.generation
    await nextTick()
    if (
      runtime.disposed ||
      revision !== bodyRestoreRevision ||
      run !== runtime.generation ||
      textarea.value !== input ||
      form.content !== state.content
    )
      return
    input?.focus()
    input?.setSelectionRange(state.selectionStart, state.selectionEnd, state.selectionDirection)
  }

  function moveBodyHistory(redo = false) {
    if (
      compositionBefore ||
      loading.value ||
      saving.value ||
      uploadingImages.value ||
      libraryBusy.value ||
      loadError.value ||
      runtime.disposed ||
      !canEdit.value ||
      !isSessionCurrent()
    )
      return
    pendingBodyInput = undefined
    const state = redo ? bodyHistory.redo(bodySnapshot()) : bodyHistory.undo(bodySnapshot())
    updateBodyHistoryState()
    if (state) void restoreBody(state)
  }

  function beforeBodyInput(event: InputEvent) {
    if (event.inputType === 'historyUndo' || event.inputType === 'historyRedo') {
      if (event.cancelable) {
        event.preventDefault()
        moveBodyHistory(event.inputType === 'historyRedo')
      }
      return
    }
    if (!compositionBefore) pendingBodyInput = bodySnapshot()
  }

  function bodyInput(event: Event) {
    const input = event.target as HTMLTextAreaElement
    const type = (event as InputEvent).inputType
    if (type === 'historyUndo' || type === 'historyRedo') {
      // 部分浏览器的原生撤销事件不可取消；仍以统一历史为准覆盖其结果。
      const before = bodySnapshot()
      moveBodyHistory(type === 'historyRedo')
      if (form.content === before.content) void restoreBody(before)
      return
    }
    const before = pendingBodyInput ?? bodySnapshot()
    form.content = input.value
    pendingBodyInput = undefined
    if (compositionBefore) return
    const group = ['insertText', 'deleteContentBackward', 'deleteContentForward'].includes(type)
      ? type
      : undefined
    bodyHistory.record(before, bodySnapshot(), group)
    updateBodyHistoryState()
  }

  function startBodyComposition() {
    compositionBefore = bodySnapshot()
    pendingBodyInput = undefined
    bodyHistory.boundary()
  }

  function endBodyComposition(event: CompositionEvent) {
    const before = compositionBefore
    compositionBefore = undefined
    if (!before) return
    form.content = (event.target as HTMLTextAreaElement).value
    bodyHistory.record(before, bodySnapshot())
    updateBodyHistoryState()
  }

  function rememberBodySelection() {
    if (!compositionBefore) bodyHistory.selection(bodySnapshot())
  }

  function bodyKeydown(event: KeyboardEvent) {
    if (event.isComposing || compositionBefore || event.keyCode === 229) return
    const key = event.key.toLowerCase()
    if ((event.ctrlKey || event.metaKey) && !event.altKey && (key === 'z' || key === 'y')) {
      event.preventDefault()
      moveBodyHistory(key === 'y' || event.shiftKey)
    } else if (
      [
        'ArrowLeft',
        'ArrowRight',
        'ArrowUp',
        'ArrowDown',
        'Home',
        'End',
        'PageUp',
        'PageDown',
      ].includes(event.key)
    ) {
      bodyHistory.boundary()
    }
  }

  function insertHeading(level: 1 | 2) {
    const input = textarea.value
    if (!input || saving.value || !canEdit.value) return
    void applyEdit(
      setMarkdownHeading(form.content, input.selectionStart, input.selectionEnd, level),
    )
  }

  function insertCode(block: boolean) {
    const input = textarea.value
    if (!input || saving.value || !canEdit.value) return
    void applyEdit(
      insertMarkdownCode(form.content, input.selectionStart, input.selectionEnd, block),
    )
  }
  return {
    bodyHistory,
    canUndoBody,
    canRedoBody,
    insertMarkdown,
    applyEdit,
    resetBodyHistory,
    moveBodyHistory,
    beforeBodyInput,
    bodyInput,
    startBodyComposition,
    endBodyComposition,
    rememberBodySelection,
    bodyKeydown,
    insertHeading,
    insertCode,
  }
}
