import { getArticlePageApi } from '@/api/article'
import {
  deleteArticleSeriesApi,
  getArticleSeriesApi,
  getSeriesDetailApi,
  saveArticleSeriesApi,
  type ArticleSeries,
  type SeriesArticle,
} from '@/api/article-series'
import { useAuthStore } from '@/stores/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

/** 系列列表与编排保持独立状态，公开只返回已发布文章，编辑时可安排自己的草稿。 */
export function useSeriesManager() {
  const auth = useAuthStore(),
    token = auth.getToken(),
    route = useRoute()
  const mine = ref(route.query.mine === '1'),
    page = ref(1),
    total = ref(0),
    items = ref<ArticleSeries[]>([])
  const loading = ref(false),
    failed = ref(false),
    visible = ref(false),
    saving = ref(false),
    editing = ref<number>()
  const searching = ref(false),
    query = ref(''),
    choices = ref<SeriesArticle[]>([]),
    searchPage = ref(1),
    searchTotal = ref(0)
  const selected = ref<SeriesArticle[]>([])
  const form = reactive({
    title: '',
    description: '',
    publicVisible: false,
    articleIds: [] as number[],
  })
  const ordered = computed(() =>
    form.articleIds.map(
      (id) =>
        selected.value.find((item) => item.id === id) ||
        choices.value.find((item) => item.id === id) || { id, title: `文章 #${id}`, status: 0 },
    ),
  )
  let generation = 0,
    searchGeneration = 0,
    editGeneration = 0,
    disposed = false
  let controller: AbortController | undefined,
    searchController: AbortController | undefined,
    editController: AbortController | undefined
  let searchTimer: ReturnType<typeof setTimeout> | undefined
  const current = () => !disposed && auth.getToken() === token
  async function load() {
    if (!current()) return
    const run = ++generation
    controller?.abort()
    controller = new AbortController()
    loading.value = true
    failed.value = false
    items.value = []
    try {
      const result = await getArticleSeriesApi(mine.value, page.value, controller.signal)
      if (!current() || run !== generation) return
      items.value = result.data.records
      total.value = result.data.total
      if (!items.value.length && page.value > 1) page.value--
    } catch {
      if (current() && run === generation) failed.value = true
    } finally {
      if (current() && run === generation) loading.value = false
    }
  }
  async function search(append = false) {
    if (!visible.value || !current()) return
    if (!append) searchPage.value = 1
    const run = ++searchGeneration
    searchController?.abort()
    searchController = new AbortController()
    searching.value = true
    try {
      const result = await getArticlePageApi(
        { userId: auth.user?.id, title: query.value, current: searchPage.value, size: 20 },
        searchController.signal,
      )
      if (!current() || run !== searchGeneration || !visible.value) return
      const found = result.data.records.map((item) => ({
        id: item.id,
        title: item.title,
        status: item.status,
      }))
      choices.value = append
        ? [
            ...choices.value,
            ...found.filter((item) => !choices.value.some((old) => old.id === item.id)),
          ]
        : found
      searchTotal.value = result.data.totalRow
    } catch {
      /* 请求拦截器提示搜索失败，已经选中的文章保持不变。 */
    } finally {
      if (current() && run === searchGeneration) searching.value = false
    }
  }
  function loadMore() {
    if (searching.value || choices.value.length >= searchTotal.value) return
    searchPage.value++
    void search(true)
  }
  async function open(item?: ArticleSeries) {
    if (saving.value || !current()) return
    const run = ++editGeneration
    editController?.abort()
    editController = new AbortController()
    editing.value = item?.id
    selected.value = []
    query.value = ''
    choices.value = []
    Object.assign(form, { title: '', description: '', publicVisible: false, articleIds: [] })
    if (item) {
      try {
        const result = await getSeriesDetailApi(item.id, true, editController.signal)
        if (!current() || run !== editGeneration) return
        selected.value = result.data.articles
        Object.assign(form, {
          ...result.data.series,
          articleIds: result.data.articles.map((article) => article.id),
        })
      } catch {
        return
      }
    }
    visible.value = true
    void search()
  }
  function add(item: SeriesArticle) {
    if (form.articleIds.includes(item.id) || saving.value || form.articleIds.length >= 100) return
    selected.value.push(item)
    form.articleIds.push(item.id)
  }
  function move(index: number, direction: number) {
    if (saving.value || index + direction < 0 || index + direction >= form.articleIds.length) return
    const next = [...form.articleIds],
      value = next[index]!
    next[index] = next[index + direction]!
    next[index + direction] = value
    form.articleIds = next
  }
  async function save() {
    if (saving.value || !current()) return
    if (!form.title.trim()) return void ElMessage.warning('请给系列起个名字')
    saving.value = true
    try {
      await saveArticleSeriesApi(editing.value, {
        title: form.title.trim(),
        description: form.description,
        publicVisible: form.publicVisible,
        articleIds: [...form.articleIds],
      })
      if (!current()) return
      visible.value = false
      ElMessage.success('系列已保存')
      await load()
    } catch {
      /* 保存失败时保留编排内容。 */
    } finally {
      if (current()) saving.value = false
    }
  }
  async function remove(item: ArticleSeries) {
    if (!current() || saving.value) return
    const yes = await ElMessageBox.confirm(
      `删除系列“${item.title}”？其中的文章会保留。`,
      '删除系列',
      { type: 'warning', confirmButtonText: '删除系列', cancelButtonText: '保留' },
    ).then(
      () => true,
      () => false,
    )
    if (!yes || !current()) return
    saving.value = true
    try {
      await deleteArticleSeriesApi(item.id)
      if (current()) await load()
    } catch {
      /* 请求失败保留列表。 */
    } finally {
      if (current()) saving.value = false
    }
  }
  watch(query, () => {
    clearTimeout(searchTimer)
    searchTimer = setTimeout(() => void search(), 300)
  })
  watch(mine, () => {
    page.value = 1
  })
  watch([mine, page], () => void load(), { immediate: true })
  watch(visible, (value) => {
    if (!value) {
      searchGeneration++
      searchController?.abort()
      clearTimeout(searchTimer)
    }
  })
  onBeforeUnmount(() => {
    disposed = true
    generation++
    searchGeneration++
    editGeneration++
    controller?.abort()
    searchController?.abort()
    editController?.abort()
    clearTimeout(searchTimer)
  })
  return {
    auth,
    mine,
    page,
    total,
    items,
    loading,
    failed,
    visible,
    saving,
    editing,
    form,
    searching,
    query,
    choices,
    searchPage,
    searchTotal,
    ordered,
    load,
    search,
    loadMore,
    open,
    add,
    move,
    save,
    remove,
  }
}
