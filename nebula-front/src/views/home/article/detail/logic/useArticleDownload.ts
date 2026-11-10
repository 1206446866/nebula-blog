import { exportArticleApi } from '@/api/article-export'
import { useAuthStore } from '@/stores/auth'
import { downloadMarkdownBlob, markdownFilename } from '@/utils/markdown-file'
import { onBeforeUnmount, ref, type Ref } from 'vue'

/** 下载当前可读文章，换号或离页后不再触发旧请求的保存对话框。 */
export function useArticleDownload(id: Ref<number>, title: Ref<string>) {
  const auth = useAuthStore(),
    token = auth.getToken(),
    downloading = ref(false)
  let disposed = false
  async function download(images: boolean) {
    if (disposed || downloading.value || auth.getToken() !== token) return
    const articleId = id.value,
      name = title.value
    downloading.value = true
    try {
      const blob = await exportArticleApi(articleId, images)
      if (!disposed && auth.getToken() === token && articleId === id.value)
        downloadMarkdownBlob(blob, markdownFilename(name, images ? 'zip' : 'md'))
    } catch {
      /* 统一请求提示下载失败，阅读状态保持不变。 */
    } finally {
      if (!disposed) downloading.value = false
    }
  }
  onBeforeUnmount(() => {
    disposed = true
  })
  return { downloading, download }
}
