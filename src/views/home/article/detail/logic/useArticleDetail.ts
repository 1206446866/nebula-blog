import { type ArticleVO, getArticleByIdApi } from '@/api/article'
import { type CategoryVO, listCategoryApi } from '@/api/category'
import { listTagApi, type TagVO } from '@/api/tag'
import { getUserById, type UserVO } from '@/api/user.ts'
import { useAuthStore } from '@/stores/auth'
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

/** 管理文章详情加载的状态与交互。 */
export function useArticleDetail() {
  const route = useRoute()
  const auth = useAuthStore()
  const articleId = computed(() => Number(route.params.id))
  let articleRequest = 0
  let controller: AbortController | undefined
  const loadError = ref(false)
  const articleLoading = ref(false)
  const article = ref<ArticleVO>()
  const currentAuthor = ref<UserVO>()
  const categories = ref<CategoryVO[]>([])
  const tags = ref<TagVO[]>([])
  const currentCategory = computed(() =>
    categories.value.find((item) => item.id === article.value?.categoryId),
  )
  const currentTags = computed(() => {
    const ids = article.value?.tagIds ?? []
    return tags.value.filter((item) => ids.includes(item.id))
  })
  const fetchArticle = async () => {
    const request = ++articleRequest
    controller?.abort()
    if (!Number.isSafeInteger(articleId.value) || articleId.value < 1) return
    const currentController = new AbortController()
    controller = currentController
    loadError.value = false
    articleLoading.value = true
    article.value = undefined
    currentAuthor.value = undefined
    try {
      const res = await getArticleByIdApi(articleId.value, currentController.signal)
      if (request !== articleRequest) return
      if (res.code !== 200) {
        currentAuthor.value = undefined
        return
      }
      article.value = res.data
      const author = await getUserById(res.data.userId, currentController.signal)
      if (request === articleRequest && author.code === 200) {
        currentAuthor.value = author.data ?? undefined
      }
    } catch {
      if (request === articleRequest && !currentController.signal.aborted) loadError.value = true
    } finally {
      if (request === articleRequest) articleLoading.value = false
    }
  }
  const fetchMeta = async () => {
    const [categoriesResult, tagsResult] = await Promise.allSettled([
      auth.hasPermission('category:list') ? listCategoryApi() : null,
      auth.hasPermission('tag:list') ? listTagApi() : null,
    ])

    const categoryRes = categoriesResult.status === 'fulfilled' ? categoriesResult.value : null
    const tagRes = tagsResult.status === 'fulfilled' ? tagsResult.value : null

    if (categoryRes?.code === 200) {
      categories.value = categoryRes.data
    }

    if (tagRes?.code === 200) {
      tags.value = tagRes.data
    }
  }

  watch(articleId, fetchArticle, { immediate: true })

  onMounted(fetchMeta)

  onBeforeUnmount(() => {
    articleRequest++
    controller?.abort()
  })

  return {
    loadError,
    articleLoading,
    article,
    currentAuthor,
    tags,
    currentCategory,
    currentTags,
    fetchArticle,
  }
}
