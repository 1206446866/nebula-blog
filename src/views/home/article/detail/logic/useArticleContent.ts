import { useMediaImages } from '@/composables/useMediaImages'
import '@/styles/markdown.css'
import { renderMarkdown } from '@/utils/markdown'
import { computed, ref } from 'vue'

/** 管理正文渲染与图片展示的状态与交互。 */
export function useArticleContent(props: Readonly<{ content: string }>) {
  const htmlContent = computed(() => renderMarkdown(props.content))
  const body = ref<HTMLElement | null>(null)

  useMediaImages(body, htmlContent)

  return { htmlContent, body }
}
