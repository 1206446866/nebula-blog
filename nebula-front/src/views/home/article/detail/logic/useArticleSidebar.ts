import { buildArticleOutline, type ArticleHeading } from '@/utils/article-outline'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

/** 管理阅读进度与章节目录的状态与交互。 */
export function useArticleSidebar(props: Readonly<{ articleId: number; content: string }>) {
  const progress = ref(0)
  const headings = ref<ArticleHeading[]>([])
  const activeId = ref('')
  const sections = computed(() => {
    const result: { heading: ArticleHeading; children: ArticleHeading[] }[] = []
    for (const heading of headings.value) {
      if (heading.level === 1) result.push({ heading, children: [] })
      else result[result.length - 1]?.children.push(heading)
    }
    return result
  })
  let articleBody: HTMLElement | null = null
  let resizeObserver: ResizeObserver | undefined
  let frame: number | undefined
  let revision = 0
  let mounted = false

  function updateReadingPosition() {
    frame = undefined
    if (!articleBody) return
    const rect = articleBody.getBoundingClientRect()
    const height = articleBody.scrollHeight
    const percent = height > 0 ? ((window.innerHeight * 0.75 - rect.top) / height) * 100 : 0
    progress.value = Math.max(0, Math.min(100, Number(percent.toFixed(1))))
    const readingLine = Math.min(160, window.innerHeight * 0.2)
    let current = headings.value[0]?.id || ''
    for (const heading of headings.value) {
      if (heading.element.getBoundingClientRect().top > readingLine) break
      current = heading.id
    }
    activeId.value = current
  }

  function scheduleUpdate() {
    if (!mounted) return
    if (frame === undefined) frame = window.requestAnimationFrame(updateReadingPosition)
  }

  async function rebuildOutline() {
    if (!mounted) return
    const run = ++revision
    resizeObserver?.disconnect()
    headings.value = []
    activeId.value = ''
    progress.value = 0
    await nextTick()
    if (!mounted || run !== revision) return
    articleBody = document.getElementById('article-content')
    if (!articleBody) return
    const body = articleBody.querySelector<HTMLElement>('.markdown-body')
    headings.value = body ? buildArticleOutline(body) : []
    if (typeof ResizeObserver !== 'undefined') {
      resizeObserver = new ResizeObserver(scheduleUpdate)
      resizeObserver.observe(articleBody)
    }
    scheduleUpdate()
  }

  function goToHeading(heading: ArticleHeading) {
    if (!heading.element.isConnected) return
    const reducedMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
    heading.element.scrollIntoView({
      behavior: reducedMotion ? 'instant' : 'smooth',
      block: 'start',
    })
    heading.element.tabIndex = -1
    heading.element.focus({ preventScroll: true })
    activeId.value = heading.id
  }

  watch(() => [props.articleId, props.content], rebuildOutline, { flush: 'post' })

  onMounted(() => {
    mounted = true
    window.addEventListener('scroll', scheduleUpdate, { passive: true })
    window.addEventListener('resize', scheduleUpdate, { passive: true })
    void rebuildOutline()
  })

  onBeforeUnmount(() => {
    mounted = false
    revision++
    resizeObserver?.disconnect()
    articleBody = null
    if (frame !== undefined) window.cancelAnimationFrame(frame)
    window.removeEventListener('scroll', scheduleUpdate)
    window.removeEventListener('resize', scheduleUpdate)
  })

  return { progress, headings, activeId, sections, goToHeading }
}
