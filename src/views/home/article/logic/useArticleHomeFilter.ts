import { listCategoryApi } from '@/api/category.ts'
import { listTagApi } from '@/api/tag.ts'
import { useArticleFilterStore } from '@/stores/articleFilter.ts'
import { useAuthStore } from '@/stores/auth'
import { articleFilterQuery } from '@/utils/article-query'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

/** 管理文章筛选栏的状态与交互。 */
export function useArticleHomeFilter() {
  const filterStore = useArticleFilterStore()
  const auth = useAuthStore()
  const router = useRouter()
  const route = useRoute()
  const syncUrl = () =>
    router.replace({
      path: '/article',
      query: { ...route.query, ...articleFilterQuery(filterStore.categoryIds, filterStore.tagIds) },
    })

  /**
   * 只要有筛选条件才显示
   */
  const visible = computed(() => {
    return filterStore.categoryIds.length || filterStore.tagIds.length
  })

  function removeCategory(id: number) {
    filterStore.removeCategory(id)
    void syncUrl()
  }

  function removeTag(id: number) {
    filterStore.removeTag(id)
    void syncUrl()
  }
  const categoryMap = ref(new Map<number, string>())
  const tagMap = ref(new Map<number, string>())

  onMounted(async () => {
    const [categories, tags] = await Promise.allSettled([
      auth.hasPermission('category:list') ? listCategoryApi() : Promise.resolve(null),
      auth.hasPermission('tag:list') ? listTagApi() : Promise.resolve(null),
    ])
    const categoryList = categories.status === 'fulfilled' ? categories.value : null
    const tagList = tags.status === 'fulfilled' ? tags.value : null
    if (categoryList?.code === 200) {
      categoryMap.value = new Map(categoryList.data.map((item) => [item.id, item.name]))
    }
    if (tagList?.code === 200) {
      tagMap.value = new Map(tagList.data.map((item) => [item.id, item.name]))
    }
  })

  return { filterStore, visible, removeCategory, removeTag, categoryMap, tagMap }
}
