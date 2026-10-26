import { useRouter } from 'vue-router'

/** 管理文章卡片跳转的状态与交互。 */
export function useArticleHomeField() {
  const router = useRouter()
  const goDetail = (id: number) => {
    router.push(`/article/${id}`)
  }

  return { goDetail }
}
