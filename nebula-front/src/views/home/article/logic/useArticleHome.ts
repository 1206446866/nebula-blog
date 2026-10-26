import { type ArticleVO, getPublishedArticlePageApi } from '@/api/article.ts'
import { useArticleFilterStore } from '@/stores/articleFilter.ts'
import { readArticleQuery } from '@/utils/article-query'
import { onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

/** 管理文章列表与筛选分页的状态与交互。 */
export function useArticleHome() {
  const articles = ref<ArticleVO[]>([])
  const currentPage = ref(1)
  const pageSize = ref(10)
  const total = ref(0)
  const filterStore = useArticleFilterStore()
  const route = useRoute()
  const router = useRouter()
  const loading = ref(false)
  const loadError = ref(false)
  let requestVersion = 0
  let controller: AbortController | undefined

  async function loadArticles() {
    const version = ++requestVersion
    controller?.abort()
    controller = new AbortController()
    loading.value = true
    loadError.value = false
    try {
      const res = await getPublishedArticlePageApi(
        {
          categoryIds: filterStore.categoryIds,
          tagIds: filterStore.tagIds,
          current: currentPage.value,
          size: pageSize.value,
        },
        controller.signal,
      )
      if (version !== requestVersion) return
      articles.value = res.data.records
      total.value = res.data.totalRow
      const last = Math.max(1, Math.ceil(total.value / pageSize.value))
      if (currentPage.value > last)
        await router.replace({ path: '/article', query: { ...route.query, page: String(last) } })
    } catch {
      if (version === requestVersion && !controller?.signal.aborted) loadError.value = true
    } finally {
      if (version === requestVersion) loading.value = false
    }
  }
  const changePage = (page: number) =>
    router.push({ path: '/article', query: { ...route.query, page: String(page) } })

  watch(
    () => route.fullPath,
    () => {
      if (route.name !== 'ArticleHome') return
      const query = readArticleQuery(route.query)
      filterStore.categoryIds = query.categoryIds
      filterStore.tagIds = query.tagIds
      currentPage.value = query.page
      articles.value = []
      void loadArticles()
    },
    { immediate: true },
  )

  onBeforeUnmount(() => {
    requestVersion++
    controller?.abort()
  })

  return { articles, currentPage, pageSize, total, loading, loadError, loadArticles, changePage }
}
