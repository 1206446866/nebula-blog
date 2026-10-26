import request from '@/utils/request'
import type { Result } from '@/types/result'
import type { Page } from '@/types/page'

export interface Relationship {
  following: boolean
  followedBy: boolean
  self: boolean
  canFollow: boolean
}
export interface FollowSummary extends Relationship {
  followingCount: number
  followerCount: number
}
export interface CommunityUser extends Relationship {
  id: number
  nid: string
  username: string
  avatar: string | null
}
export type CommunityMode = 'discover' | 'following' | 'followers'

export const getFollowSummary = (id: number, signal?: AbortSignal) =>
  request.get<Result<FollowSummary>>(`/community/users/${id}/relationship`, { signal })

export const setFollowing = (id: number, following: boolean) =>
  following
    ? request.put<Result<FollowSummary>>(`/community/users/${id}/following`)
    : request.delete<Result<FollowSummary>>(`/community/users/${id}/following`)

export function getCommunityUsers(
  mode: CommunityMode,
  userId: number,
  current: number,
  keyword: string,
  signal?: AbortSignal,
) {
  const url = mode === 'discover' ? '/community/users' : `/community/users/${userId}/${mode}`
  return request.get<Result<Page<CommunityUser>>>(url, {
    params: { current, size: 12, ...(mode === 'discover' ? { keyword } : {}) },
    signal,
  })
}
