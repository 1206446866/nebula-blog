import { type ArticleVO, likeArticleApi } from '@/api/article.ts'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'

export interface ArticleNavItem {
  id: number
  title: string
}

/** 管理文章导航与点赞的状态与交互。 */
export function useArticleNavigation(
  props: Readonly<{
    prevArticle?: ArticleNavItem | null
    nextArticle?: ArticleNavItem | null
    article: ArticleVO
  }>,
) {
  const articleState = ref({
    ...props.article,
  })
  const liking = ref(false)
  const auth = useAuthStore()

  watch(
    () => props.article,
    (article) => {
      articleState.value = { ...article }
    },
  )
  const router = useRouter()
  const goList = () => {
    router.push('/article')
  }
  const goArticle = (id?: number) => {
    if (id) void router.push('/article/' + id)
  }
  const like = async () => {
    if (liking.value || props.article.status !== 1) return
    if (!auth.getToken()) {
      ElMessage.warning('请先登录')
      return
    }

    const articleId = props.article.id
    liking.value = true
    try {
      const res = await likeArticleApi({ articleId })
      if (props.article.id === articleId && res.code === 200) {
        articleState.value.liked = res.data
        ElMessage.success(res.data ? '点赞成功 ❤️' : '取消点赞')
      }
    } catch {
      // 请求拦截器统一显示错误，失败时保留原有点赞状态。
    } finally {
      liking.value = false
    }
  }

  return { articleState, liking, goList, goArticle, like }
}
