import {
  getArticleRevisionApi,
  getArticleRevisionsApi,
  type ArticleRevision,
} from '@/api/article-revision'
import type { WritingDraft } from '@/api/article-workspace'
import { useAuthStore } from '@/stores/auth'
import { onBeforeUnmount, ref, watch, type Ref } from 'vue'

/** 历史版本按需加载，预览与恢复都不直接写入正式文章。 */
export function useArticleRevisions(id: Ref<number | undefined>) {
  const auth = useAuthStore(),
    token = auth.getToken()
  const visible = ref(false),
    loading = ref(false),
    failed = ref(false)
  const items = ref<ArticleRevision[]>([]),
    selected = ref<number>(),
    draft = ref<WritingDraft>()
  let generation = 0,
    disposed = false,
    controller: AbortController | undefined
  const current = (run = generation) => !disposed && run === generation && auth.getToken() === token
  async function load() {
    if (!id.value || auth.getToken() !== token) return
    const run = ++generation
    controller?.abort()
    controller = new AbortController()
    loading.value = true
    failed.value = false
    draft.value = undefined
    selected.value = undefined
    try {
      const result = await getArticleRevisionsApi(id.value, controller.signal)
      if (current(run)) items.value = result.data
    } catch {
      if (current(run)) failed.value = true
    } finally {
      if (current(run)) loading.value = false
    }
  }
  async function select(revision: number) {
    if (!id.value || !current()) return
    const run = ++generation
    controller?.abort()
    controller = new AbortController()
    loading.value = true
    draft.value = undefined
    failed.value = false
    selected.value = revision
    try {
      const result = await getArticleRevisionApi(id.value, revision, controller.signal)
      if (current(run)) draft.value = result.data.draft
    } catch {
      if (current(run)) failed.value = true
    } finally {
      if (current(run)) loading.value = false
    }
  }
  watch(visible, (value) => {
    if (value) void load()
    else {
      generation++
      controller?.abort()
      draft.value = undefined
      items.value = []
    }
  })
  watch(id, () => {
    visible.value = false
    generation++
    controller?.abort()
    items.value = []
    draft.value = undefined
  })
  onBeforeUnmount(() => {
    disposed = true
    generation++
    controller?.abort()
  })
  return { visible, loading, failed, items, selected, draft, load, select }
}
