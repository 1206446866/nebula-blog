// 个人主页相关接口。
import request from '@/utils/request'
import type { Page } from '@/types/page.ts'
import type { Result } from '@/types/result.ts'
import type { PublicUserVO } from './user'

export interface ProfileQuery {
  publicArticlePage?: number
  publicArticleSize?: number
  draftArticlePage?: number
  draftArticleSize?: number
  commentPage?: number
  commentSize?: number
}

export interface PublicUserProfileVO {
  userProfileInfoVO: PublicUserVO
  statisticsVO: UserProfileStatisticsVO
  publicArticles: Page<UserProfileArticleVO>
  comments: Page<UserProfileCommentVO>
}

export interface UserProfileVO extends PublicUserProfileVO {
  userProfileInfoVO: UserProfileInfoVO
  loginLogs: UserProfileLoginLogVO[]
  draftArticles: Page<UserProfileArticleVO> | null
}

export type ProfileResponse = PublicUserProfileVO | UserProfileVO

export function signedProfile(profile: ProfileResponse | null): UserProfileVO | null {
  return profile && 'loginLogs' in profile ? profile : null
}

export interface UserProfileInfoVO extends PublicUserVO {
  nid: string
  status: number
  roles: string[]
  email: string | null
  phone: string | null
}

export interface UserProfileStatisticsVO {
  articleCount: number
  commentCount: number
  totalViewCount: number
  likeCount: number
}

export interface UserProfileLoginLogVO {
  id: number
  /**
   * 用户ID
   */
  userId: number

  /**
   * 登录方式
   */
  loginType: string

  /**
   * 登录设备
   */
  device: string

  /**
   * IP地址
   */
  ip: string

  /**
   * 登录地区
   */
  location: string

  /**
   * 登录状态 0失败 1成功
   */
  status: number

  /**
   * 登录时间
   */
  createTime: string

  /**
   * 浏览器
   */
  browser: string

  /**
   * 操作系统
   */
  os: string
}

export interface UserProfileArticleVO {
  id: number
  title: string
  content: string | null
  viewCount: number
  createTime: string
  likeCount: number
  scheduledPublishAt?: string | null
  scheduleError?: string | null
  comments: number
}

export interface UserProfileCommentVO {
  id: number
  articleId: number
  content: string
  articleTitle: string | null
  createTime: string
  likeCount: number
}
export const getProfileApi = (id: number, params: ProfileQuery, signal?: AbortSignal) => {
  return request.get<Result<ProfileResponse>>(`/api/profile/${id}`, { params, signal, publicRead: true })
}
