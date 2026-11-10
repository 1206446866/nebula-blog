import { getSeriesDetailApi, type SeriesDetail } from '@/api/article-series'
import { useAuthStore } from '@/stores/auth'
import { onBeforeUnmount, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { positiveId } from '@/utils/article-query'
export function useSeriesDetail() {
  const route = useRoute(),
    auth = useAuthStore(),
    token = auth.getToken()
  const detail = ref<SeriesDetail>(),
    loading = ref(false),
    failed = ref(false)
  let generation = 0,
    disposed = false,
    controller: AbortController | undefined
  async function load() {
    const run = ++generation
    controller?.abort()
    controller = new AbortController()
    loading.value = true
    failed.value = false
    detail.value = undefined
    try {
      if (!positiveId(route.params.id)) throw new Error('系列编号无效')
      const result = await getSeriesDetailApi(Number(route.params.id), false, controller.signal)
      if (!disposed && generation === run && token === auth.getToken()) detail.value = result.data
    } catch {
      if (!disposed && generation === run) failed.value = true
    } finally {
      if (!disposed && generation === run) loading.value = false
    }
  }
  watch(
    () => route.params.id,
    () => void load(),
    { immediate: true },
  )
  onBeforeUnmount(() => {
    disposed = true
    generation++
    controller?.abort()
  })
  return { detail, loading, failed, load }
}
