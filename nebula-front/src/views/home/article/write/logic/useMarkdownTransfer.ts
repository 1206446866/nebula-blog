import { archiveWritingApi } from '@/api/article-export'
import {
  downloadMarkdownBlob,
  exportMarkdown,
  importMarkdown,
  markdownFilename,
} from '@/utils/markdown-file'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ref } from 'vue'
import type { WritingDraft } from '@/api/article-workspace'

/** 文件只进入编辑器，导入与下载都不会自动保存或发布文章。 */
export function useMarkdownTransfer(context: {
  current: () => boolean
  documentKey: () => number
  busy: () => boolean
  dirty: () => boolean
  canEdit: () => boolean
  draft: () => WritingDraft
  replace: (draft: WritingDraft) => Promise<boolean | void>
}) {
  const fileInput = ref<HTMLInputElement>(),
    transferring = ref(false)
  function choose() {
    if (context.current() && !context.busy() && context.canEdit()) fileInput.value?.click()
  }
  async function importFile(event: Event) {
    const input = event.target as HTMLInputElement,
      file = input.files?.[0]
    input.value = ''
    if (!file || context.busy() || !context.current() || !context.canEdit()) return
    if (!/\.(md|markdown)$/i.test(file.name) || file.size > 800000)
      return void ElMessage.warning('请选择不超过800KB的.md或.markdown文件')
    transferring.value = true
    const key = context.documentKey()
    try {
      const value = importMarkdown(await file.text(), file.name)
      if (!context.current() || context.documentKey() !== key) return
      if (context.dirty()) {
        const yes = await ElMessageBox.confirm(
          '导入会替换编辑器中的标题和正文，正文可撤销；分类与标签会保留，不会自动发布。',
          '导入Markdown',
          { confirmButtonText: '导入到编辑器', cancelButtonText: '保留当前内容', type: 'warning' },
        ).then(
          () => true,
          () => false,
        )
        if (!yes || !context.current() || context.documentKey() !== key) return
      }
      if (context.busy()) return
      const applied = await context.replace({ ...context.draft(), ...value })
      if (applied === false) return
      if (context.current()) ElMessage.success('已导入到编辑器，请检查图片和目录关联后手动保存')
    } catch (error) {
      if (context.current())
        ElMessage.warning(error instanceof Error ? error.message : '文件无法读取')
    } finally {
      transferring.value = false
    }
  }
  function exportFile() {
    if (!context.current() || context.busy()) return
    const value = context.draft()
    downloadMarkdownBlob(
      new Blob([exportMarkdown(value.title, value.content)], {
        type: 'text/markdown;charset=utf-8',
      }),
      markdownFilename(value.title),
    )
  }
  async function exportArchive() {
    if (!context.current() || context.busy() || transferring.value) return
    transferring.value = true
    const value = context.draft()
    try {
      const blob = await archiveWritingApi(value.title, value.content)
      if (context.current()) downloadMarkdownBlob(blob, markdownFilename(value.title, 'zip'))
    } catch {
      /* 下载失败时保留编辑内容，统一请求提示。 */
    } finally {
      transferring.value = false
    }
  }
  return { fileInput, transferring, choose, importFile, exportFile, exportArchive }
}
