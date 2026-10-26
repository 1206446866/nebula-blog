import { type CategoryVO, listCategoryApi } from '@/api/category'
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

/** 管理分类查询与跳转的状态与交互。 */
export function useCategoryPage() {
  const router = useRouter()
  const keyword = ref('')
  const categories = ref<CategoryVO[]>([])
  const fetchCategories = async () => {
    const res = await listCategoryApi()
    if (res.code === 200) {
      categories.value = res.data
    } else {
      ElMessage.error(res.message)
    }
  }

  onMounted(fetchCategories)
  const filteredCategories = computed(() => {
    if (!keyword.value) return categories.value
    return categories.value.filter(
      (item) =>
        item.name.includes(keyword.value) ||
        (item.description && item.description.includes(keyword.value)),
    )
  })

  // 给不同分类分配默认图标
  const getIcon = (name: string) => {
    const map: Record<string, string> = {
      前端: '💻',
      后端: '⚙️',
      数据库: '🗄️',
      云与运维: '☁️',
      人工智能: '🤖',
      安全: '🔐',
    }
    return map[name] || '📂'
  }

  // 点击分类跳转文章列表页
  const goToCategoryArticles = (categoryId: number) => {
    router.push({ path: '/article', query: { categoryId: String(categoryId) } })
  }

  return { keyword, filteredCategories, getIcon, goToCategoryArticles }
}
