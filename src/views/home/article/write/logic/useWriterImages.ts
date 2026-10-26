import { uploadArticleImageApi, type MediaAsset } from '@/api/media'
import type { MarkdownEdit } from '@/utils/markdown-edit'
import { imageMarkdown } from '@/utils/resource'
import { ElMessage } from 'element-plus'
import { ref } from 'vue'
import type { WriterEditorContext } from './types'

/** 处理上传、粘贴、拖入和媒体库插图，通过正文编辑入口保留撤销能力。 */
export function useWriterImages(
  context: WriterEditorContext & { applyEdit: (edit: MarkdownEdit) => Promise<void> },
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
    applyEdit,
  } = context
  const imageInput = ref<HTMLInputElement | null>(null)
  const mediaOpen = ref(false)
  let imageController: AbortController | undefined
  let imagePosition = { start: 0, end: 0, source: '' }

  function captureImagePosition() {
    imagePosition = {
      start: textarea.value?.selectionStart ?? form.content.length,
      end: textarea.value?.selectionEnd ?? form.content.length,
      source: form.content,
    }
  }

  function openMedia() {
    if (saving.value || uploadingImages.value || !canEdit.value) return
    captureImagePosition()
    mediaOpen.value = true
  }

  function chooseImage() {
    if (saving.value || uploadingImages.value || !canEdit.value) return
    captureImagePosition()
    imageInput.value?.click()
  }

  async function insertImages(assets: MediaAsset[]) {
    if (!assets.length) return
    if (runtime.disposed || !isSessionCurrent() || !canEdit.value) return
    const start =
      form.content === imagePosition.source
        ? imagePosition.start
        : (textarea.value?.selectionStart ?? form.content.length)
    const end = form.content === imagePosition.source ? imagePosition.end : start
    const text = assets.map((asset) => imageMarkdown(asset.path, asset.originalName)).join('\n\n')
    const insertion = `\n\n${text}\n\n`
    await applyEdit({
      content: form.content.slice(0, start) + insertion + form.content.slice(end),
      selectionStart: start + insertion.length,
      selectionEnd: start + insertion.length,
    })
  }

  async function selectMedia(asset: MediaAsset) {
    mediaOpen.value = false
    await insertImages([asset])
  }

  async function uploadImages(files: File[]) {
    if (
      !files.length ||
      uploadingImages.value ||
      saving.value ||
      !canEdit.value ||
      !isSessionCurrent()
    )
      return
    if (files.length > 5) return void ElMessage.warning('一次最多上传5张图片')
    if (
      files.some(
        (file) =>
          !/^image\/(jpeg|png|gif|webp)$/.test(file.type) ||
          !file.size ||
          file.size > 10 * 1024 * 1024,
      )
    )
      return void ElMessage.warning('请选择不超过10MB的 JPG、PNG、GIF 或 WebP 图片')
    uploadingImages.value = true
    imageController = new AbortController()
    const run = runtime.generation
    const assets: MediaAsset[] = []
    try {
      for (const file of files) {
        const result = await uploadArticleImageApi(file, imageController.signal)
        if (runtime.disposed || runtime.generation !== run || !isSessionCurrent()) return
        assets.push(result.data)
      }
    } catch {
      if (!runtime.disposed && runtime.generation === run && isSessionCurrent())
        ElMessage.warning('部分图片未上传成功，已上传的图片会保留在媒体库中')
    } finally {
      if (!runtime.disposed && runtime.generation === run && isSessionCurrent()) {
        await insertImages(assets)
        uploadingImages.value = false
      }
    }
  }

  function imageChanged(event: Event) {
    const input = event.target as HTMLInputElement
    const files = Array.from(input.files || [])
    input.value = ''
    void uploadImages(files)
  }

  function pasteImages(event: ClipboardEvent) {
    const files = Array.from(event.clipboardData?.files || []).filter((file) =>
      file.type.startsWith('image/'),
    )
    if (!files.length) return
    event.preventDefault()
    captureImagePosition()
    void uploadImages(files)
  }

  function dropImages(event: DragEvent) {
    const files = Array.from(event.dataTransfer?.files || [])
    if (!files.length) return
    event.preventDefault()
    captureImagePosition()
    void uploadImages(files)
  }

  function closeMedia(done: () => void) {
    if (libraryBusy.value) return void ElMessage.info('图片正在上传，请稍候')
    done()
  }

  function resetImageUpload() {
    imageController?.abort()
    uploadingImages.value = false
  }
  function disposeImages() {
    imageController?.abort()
  }
  return {
    imageInput,
    mediaOpen,
    captureImagePosition,
    openMedia,
    chooseImage,
    selectMedia,
    uploadImages,
    imageChanged,
    pasteImages,
    dropImages,
    closeMedia,
    resetImageUpload,
    disposeImages,
  }
}
