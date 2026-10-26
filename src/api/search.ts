import request from '@/utils/request'
import type { Result } from '@/types/result'
import type { Page } from '@/types/page'
import type { SearchType } from '@/utils/site-search'

export interface SearchItem {
  type: Exclude<SearchType, 'all'>
  id: number
  targetId: number
  title: string
  excerpt: string | null
  avatar: string | null
  createTime: string | null
}
export interface SearchResult {
  page: Page<SearchItem>
  counts: Record<Exclude<SearchType, 'all'>, number>
}
export const searchSite = (q: string, type: SearchType, current: number, signal?: AbortSignal) =>
  request.get<Result<SearchResult>>('/search', { params: { q, type, current, size: 12 }, signal })
