import {
  getReadingApi,
  saveReadingProgressApi,
  setReadingFlagsApi,
  type ReadingState,
} from '@/api/reading'
import { useAuthStore } from '@/stores/auth'
import { computed, nextTick, onBeforeUnmount, ref, watch, type Ref } from 'vue'
import { captureReadingPosition, restoreReadingPosition } from './reading-position'

/** 收藏与稍后读独立于点赞；阅读位置只在用户选择继续或从头阅读后更新。 */
export function useReadingTools(articleId: Ref<number>, enabled: Ref<boolean>) {
  const auth = useAuthStore(),
    token = auth.getToken()
  const state = ref<ReadingState>({
    favorite: false,
    later: false,
    progress: 0,
    anchor: '',
    readAt: null,
  })
  const ready = ref(false),
    busy = ref(false),
    resumePending = ref(false),
    error = ref('')
  const resumePercent = computed(() => Math.round(state.value.progress / 100))
  let generation = 0,
    disposed = false,
    controller: AbortController | undefined
  let progressFlight = false,
    moved = false
  const current = (run = generation) => !disposed && generation === run && auth.getToken() === token
  const body = () => document.querySelector<HTMLElement>('#article-content .markdown-body')

  async function load() {
    const run = ++generation
    controller?.abort()
    controller = new AbortController()
    ready.value = false
    resumePending.value = false
    busy.value = false
    error.value = ''
    moved = false
    if (!enabled.value || !token || auth.getToken() !== token) return
    try {
      const result = await getReadingApi(articleId.value, controller.signal)
      if (!current(run)) return
      state.value = result.data
      resumePending.value = result.data.progress > 300 && result.data.progress < 9900
      ready.value = true
      await nextTick()
      if (current(run) && !resumePending.value) void saveProgress(true)
    } catch {
      if (current(run)) error.value = '暂时无法读取书架状态'
    }
  }

  async function toggle(kind: 'favorite' | 'later') {
    if (!ready.value || busy.value || !current()) return
    const run = generation
    busy.value = true
    try {
      const flags = {
        favorite: state.value.favorite,
        later: state.value.later,
        [kind]: !state.value[kind],
      }
      const result = await setReadingFlagsApi(articleId.value, flags)
      if (current(run))
        state.value = { ...state.value, favorite: result.data.favorite, later: result.data.later }
    } catch {
      /* 请求拦截器提示错误，保留原标记。 */
    } finally {
      if (current(run)) busy.value = false
    }
  }

  async function saveProgress(force = false) {
    if (
      !current() ||
      !ready.value ||
      resumePending.value ||
      !enabled.value ||
      progressFlight ||
      (!moved && !force)
    )
      return
    const content = body()
    if (!content) return
    const { progress, anchor } = captureReadingPosition(content, window.scrollY, window.innerHeight)
    const run = generation,
      id = articleId.value
    progressFlight = true
    moved = false
    try {
      const result = await saveReadingProgressApi(id, progress, anchor)
      if (current(run))
        state.value = {
          ...state.value,
          progress: result.data.progress,
          anchor: result.data.anchor,
          readAt: result.data.readAt,
        }
    } catch {
      if (current(run)) moved = true
    } finally {
      progressFlight = false
    }
  }

  async function resume(fromStart = false) {
    if (!current() || !ready.value) return
    const run = generation
    if (!body()) return
    resumePending.value = false
    // 恢复提示收起后再计算正文位置，避免提示本身的高度造成阅读位置偏移。
    await nextTick()
    if (!current(run) || !ready.value) return
    const content = body()
    if (!content) return
    const position = fromStart
      ? content.getBoundingClientRect().top + window.scrollY - 112
      : restoreReadingPosition(
          content,
          state.value.progress,
          state.value.anchor,
          window.scrollY,
          window.innerHeight,
        )
    window.scrollTo({ top: Math.max(0, position), behavior: 'instant' })
    moved = true
    void saveProgress(true)
  }

  const scroll = () => {
    if (ready.value && !resumePending.value) moved = true
  }
  const visibility = () => {
    if (document.visibilityState === 'hidden') void saveProgress()
  }
  watch([articleId, enabled], () => void load(), { immediate: true })
  const timer = setInterval(() => void saveProgress(), 15000)
  window.addEventListener('scroll', scroll, { passive: true })
  window.addEventListener('pagehide', visibility)
  document.addEventListener('visibilitychange', visibility)
  onBeforeUnmount(() => {
    void saveProgress()
    disposed = true
    generation++
    controller?.abort()
    clearInterval(timer)
    window.removeEventListener('scroll', scroll)
    window.removeEventListener('pagehide', visibility)
    document.removeEventListener('visibilitychange', visibility)
  })
  return { state, ready, busy, resumePending, resumePercent, error, load, toggle, resume }
}
