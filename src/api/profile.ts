// api/profile.ts
import request from '@/utils/request'
import type { Page } from '@/types/page.ts'

export interface ProfileQuery {
  publicArticlePage?: number
  publicArticleSize?: number
  draftArticlePage?: number
  draftArticleSize?: number
  commentPage?: number
  commentSize?: number
}

export interface UserProfileVO {
  userProfileInfoVO: UserProfileInfoVO
  statisticsVO: UserProfileStatisticsVO
  loginLogs: UserProfileLoginLogVO[]
  publicArticles: Page<UserProfileArticleVO>
  comments: Page<UserProfileCommentVO>
  draftArticles: Page<UserProfileArticleVO>
}

export interface UserProfileInfoVO {
  id: number
  nid: string
  username: string
  avatar: string
  status: number
  roles: string[]
  email: string
  phone: string
  createTime: string
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
  content: string
  viewCount: number
  createTime: string
  likeCount: number
  comments: number
}

export interface UserProfileCommentVO {
  id: number
  articleId: number
  content: string
  articleTitle: string
  createTime: string
  likeCount: number
}
export const getProfileApi = (id: number, params: ProfileQuery) => {
  return request.get(`/api/profile/${id}`, { params })
}
