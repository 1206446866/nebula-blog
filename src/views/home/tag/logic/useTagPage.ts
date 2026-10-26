import { getTagHome, type TagVO } from '@/api/tag'
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

/** 管理标签查询与跳转的状态与交互。 */
export function useTagPage() {
  const router = useRouter()
  const tags = ref<TagVO[]>([])
  const loading = ref(true)
  const error = ref('')
  let disposed = false
  let requestVersion = 0
  const fetchTags = async () => {
    const version = ++requestVersion
    loading.value = true
    error.value = ''
    try {
      const res = await getTagHome()
      if (disposed || version !== requestVersion) return
      if (res.code === 200 && Array.isArray(res.data)) tags.value = res.data
      else error.value = '星图暂时无法连接，请稍后再试。'
    } catch {
      if (!disposed && version === requestVersion) error.value = '星图暂时无法连接，请稍后再试。'
    } finally {
      if (!disposed && version === requestVersion) loading.value = false
    }
  }
  const goToTagArticles = (tagId: number) => {
    router.push({ path: '/article', query: { tagId: String(tagId) } })
  }

  onMounted(() => {
    void fetchTags()
  })
  onBeforeUnmount(() => {
    disposed = true
  })

  return { tags, loading, error, refresh: fetchTags, goToTagArticles }
}
