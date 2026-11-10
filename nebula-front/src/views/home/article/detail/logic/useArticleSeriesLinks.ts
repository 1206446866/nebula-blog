import {
  getArticleSeriesLinksApi,
  getSeriesDetailApi,
  type ArticleSeries,
  type SeriesDetail,
} from '@/api/article-series'
import { useAuthStore } from '@/stores/auth'
import { computed, onBeforeUnmount, ref, watch, type Ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

/** 只采用服务端返回的公开系列，地址中的系列编号不能扩展文章可见范围。 */
export function useArticleSeriesLinks(id: Ref<number>) {
  const auth = useAuthStore(),
    token = auth.getToken(),
    route = useRoute(),
    router = useRouter()
  const series = ref<ArticleSeries[]>([]),
    detail = ref<SeriesDetail>(),
    selected = ref<number>()
  let generation = 0,
    disposed = false,
    controller: AbortController | undefined
  const current = (run: number) => !disposed && generation === run && auth.getToken() === token
  const index = computed(
    () => detail.value?.articles.findIndex((item) => item.id === id.value) ?? -1,
  )
  const previous = computed(() =>
    index.value > 0 ? detail.value?.articles[index.value - 1] : undefined,
  )
  const next = computed(() =>
    index.value >= 0 ? detail.value?.articles[index.value + 1] : undefined,
  )
  async function load() {
    const run = ++generation
    controller?.abort()
    controller = new AbortController()
    series.value = []
    detail.value = undefined
    selected.value = undefined
    try {
      const result = await getArticleSeriesLinksApi(id.value, controller.signal)
      if (!current(run)) return
      series.value = result.data
      selected.value =
        result.data.find((item) => String(item.id) === route.query.series)?.id || result.data[0]?.id
      if (!selected.value) return
      const found = await getSeriesDetailApi(selected.value, false, controller.signal)
      if (current(run)) detail.value = found.data
    } catch {
      /* 系列是附属导航，失败时不影响文章正文阅读。 */
    }
  }
  async function choose(value: string) {
    const selectedId = Number(value)
    if (!series.value.some((item) => item.id === selectedId)) return
    await router.replace({ query: { ...route.query, series: String(selectedId) } })
  }
  watch([id, () => route.query.series], () => void load(), { immediate: true })
  onBeforeUnmount(() => {
    disposed = true
    generation++
    controller?.abort()
  })
  return { series, detail, selected, previous, next, index, choose }
}
