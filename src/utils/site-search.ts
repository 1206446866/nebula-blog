export const searchTypes = ['all', 'article', 'comment', 'user', 'category', 'tag'] as const
export type SearchType = (typeof searchTypes)[number]

export function readSiteSearchQuery(query: Record<string, unknown>) {
  const q = query.q ?? ''
  const type = query.type ?? 'all'
  const page = query.page ?? '1'
  if (
    typeof q !== 'string' ||
    q.length > 50 ||
    !searchTypes.includes(type as SearchType) ||
    typeof page !== 'string' ||
    !/^[1-9]\d*$/.test(page) ||
    Number(page) > 2147483647
  )
    throw new Error('搜索地址无效，请重新输入关键词。')
  return { q: q.trim(), type: type as SearchType, page: Number(page) }
}

export function searchTarget(item: { type: string; targetId: number }) {
  const id = item.targetId
  if (!Number.isSafeInteger(id) || id < 1) return '/404'
  switch (item.type) {
    case 'article':
      return `/article/${id}`
    case 'comment':
      return `/article/${id}#article-comments`
    case 'user':
      return `/user/${id}`
    case 'category':
      return `/article?categoryIds=${id}`
    case 'tag':
      return `/article?tagIds=${id}`
    default:
      return '/404'
  }
}
