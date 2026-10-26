import { searchSite, type SearchResult } from '@/api/search'
import { useAuthStore } from '@/stores/auth'
import { readSiteSearchQuery, type SearchType } from '@/utils/site-search'
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

/** 管理站内搜索与分页的状态与交互。 */
export function useSiteSearch() {
  const route = useRoute(),
    router = useRouter(),
    auth = useAuthStore()
  const keyword = ref('')
  const query = ref({ q: '', type: 'all' as SearchType, page: 1 })
  const result = ref<SearchResult | null>(null)
  const loading = ref(false),
    error = ref('')
  const labels: Record<SearchType, string> = {
    all: '全部',
    article: '文章',
    comment: '评论',
    user: '用户',
    category: '分类',
    tag: '标签',
  }
  const tabs = computed(() =>
    Object.entries(labels)
      .filter(
        ([type]) => !['article', 'comment'].includes(type) || auth.hasPermission('article:list'),
      )
      .filter(([type]) => type !== 'category' || auth.hasPermission('category:list'))
      .filter(([type]) => type !== 'tag' || auth.hasPermission('tag:list')),
  )
  const total = computed(() =>
    result.value ? Object.values(result.value.counts).reduce((sum, count) => sum + count, 0) : 0,
  )
  let version = 0,
    controller: AbortController | undefined

  async function load() {
    const run = ++version
    controller?.abort()
    controller = new AbortController()
    const signal = controller.signal
    loading.value = false
    error.value = ''
    result.value = null
    try {
      query.value = readSiteSearchQuery(route.query)
    } catch {
      error.value = '搜索地址无效，请重新输入关键词。'
      return
    }
    keyword.value = query.value.q
    if (!query.value.q) return
    loading.value = true
    try {
      const response = await searchSite(query.value.q, query.value.type, query.value.page, signal)
      if (run === version) result.value = response.data
    } catch {
      if (run === version && !signal.aborted) error.value = '搜索暂时不可用，请重试。'
    } finally {
      if (run === version) loading.value = false
    }
  }

  function navigate(type: string = query.value.type, page = 1) {
    void router.push({
      name: 'SiteSearch',
      query: { q: query.value.q, type, ...(page > 1 ? { page } : {}) },
    })
  }

  function submit() {
    void router.push({ name: 'SiteSearch', query: { q: keyword.value.trim(), type: 'all' } })
  }

  watch(() => [route.query.q, route.query.type, route.query.page, auth.getToken()], load, {
    immediate: true,
  })

  onBeforeUnmount(() => {
    version++
    controller?.abort()
  })

  return { keyword, query, result, loading, error, labels, tabs, total, load, navigate, submit }
}
