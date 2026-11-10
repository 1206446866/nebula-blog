import {
  clearReadingHistoryApi,
  getReadingLibraryApi,
  setReadingFlagsApi,
  type LibraryKind,
  type ReadingItem,
} from '@/api/reading'
import { useAuthStore } from '@/stores/auth'
import { ElMessageBox } from 'element-plus'
import { onBeforeUnmount, ref, watch } from 'vue'

/** 管理本人的书架分页，换号、切换栏目和离页后丢弃过期响应。 */
export function useReadingLibrary() {
  const auth = useAuthStore(),
    token = auth.getToken()
  const kind = ref<LibraryKind>('favorite'),
    page = ref(1),
    total = ref(0),
    items = ref<ReadingItem[]>([])
  const loading = ref(false),
    failed = ref(false),
    acting = ref<number>(),
    clearing = ref(false)
  let generation = 0,
    disposed = false,
    controller: AbortController | undefined
  const current = (run = generation) => !disposed && run === generation && auth.getToken() === token
  async function load() {
    const run = ++generation
    controller?.abort()
    controller = new AbortController()
    loading.value = true
    failed.value = false
    items.value = []
    try {
      const result = await getReadingLibraryApi(kind.value, page.value, controller.signal)
      if (!current(run)) return
      items.value = result.data.records
      total.value = result.data.total
      if (!items.value.length && page.value > 1) page.value--
    } catch {
      if (current(run)) failed.value = true
    } finally {
      if (current(run)) loading.value = false
    }
  }
  async function remove(item: ReadingItem) {
    if (acting.value || kind.value === 'history' || !current()) return
    acting.value = item.articleId
    const run = generation
    try {
      await setReadingFlagsApi(item.articleId, {
        favorite: kind.value === 'favorite' ? false : item.state.favorite,
        later: kind.value === 'later' ? false : item.state.later,
      })
      if (current(run)) await load()
    } catch {
      /* 请求失败时保留原列表。 */
    } finally {
      if (!disposed) acting.value = undefined
    }
  }
  async function clear() {
    if (clearing.value || !current()) return
    const accepted = await ElMessageBox.confirm(
      '清除阅读历史与续读位置，收藏和稍后读会保留。',
      '清除阅读历史',
      { type: 'warning', confirmButtonText: '清除历史', cancelButtonText: '保留' },
    ).then(
      () => true,
      () => false,
    )
    if (!accepted || !current()) return
    clearing.value = true
    try {
      await clearReadingHistoryApi()
      if (current()) {
        page.value = 1
        await load()
      }
    } catch {
      /* 请求拦截器提示失败。 */
    } finally {
      if (!disposed) clearing.value = false
    }
  }
  watch(kind, () => {
    page.value = 1
  })
  watch([kind, page], () => void load(), { immediate: true })
  onBeforeUnmount(() => {
    disposed = true
    generation++
    controller?.abort()
  })
  return { kind, page, total, items, loading, failed, acting, clearing, load, remove, clear }
}
