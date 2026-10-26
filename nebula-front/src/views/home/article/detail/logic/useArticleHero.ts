import type { ArticleVO } from '@/api/article'
import type { CategoryVO } from '@/api/category'
import type { TagVO } from '@/api/tag'
import type { UserVO } from '@/api/user'
import router from '@/router'
import { avatarUrl as resourceAvatarUrl } from '@/utils/resource'
import { computed } from 'vue'

/** 管理文章头部信息的状态与交互。 */
export function useArticleHero(
  props: Readonly<{
    article: ArticleVO
    category?: CategoryVO
    tags: TagVO[]
    author?: UserVO
  }>,
) {
  const avatarUrl = computed(() => resourceAvatarUrl(props.author?.avatar))
  const readingMinutes = computed(() =>
    Math.max(1, Math.ceil(props.article.content.replace(/\s/g, '').length / 400)),
  )
  const compactCount = (count: number) =>
    new Intl.NumberFormat('zh-CN', { notation: 'compact', maximumFractionDigits: 1 }).format(
      count || 0,
    )
  const goAuthor = () => {
    if (props.author) void router.push('/user/' + props.article.userId)
  }
  const goCategory = () => {
    if (props.category)
      void router.push({ path: '/article', query: { categoryId: props.category.id } })
  }
  const goTag = (tagId: number) => router.push({ path: '/article', query: { tagId } })

  return { avatarUrl, readingMinutes, compactCount, goAuthor, goCategory, goTag }
}
