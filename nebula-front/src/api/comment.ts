import request from '@/utils/request'
import type { Result } from '@/types/result'
import type { Page } from '@/types/page'

export interface CommentVO {
  id: number
  articleId: number
  userId: number
  username: string
  content: string
  createTime: string
  liked: boolean
  parentId?: number
  rootId?: number
  replyToUserId?: number
  replyToUsername?: string
  avatar?: string
  likeCount?: number
  replyCount?: number
}

/**
 * 发布评论DTO
 */
export interface ReleaseCommentDto {
  articleId: number
  content: string
  parentId?: number
}

export const getCommentPageApi = (
  articleId?: number,
  content?: string,
  currentPage = 1,
  size = 10,
  signal?: AbortSignal,
) => {
  return request.get<Result<Page<CommentVO>>>('/comments', {
    signal,
    params: {
      articleId,
      content,
      currentPage,
      size,
    },
  })
}

/**
 * 发布评论
 */
export const releaseCommentApi = (data: ReleaseCommentDto) => {
  return request.put<Result<boolean>>('/comments/release', data)
}

export const getCommentThreadsApi = (articleId: number, currentPage = 1, size = 10) =>
  request.get<Result<Page<CommentVO>>>('/comments/threads', {
    params: { articleId, currentPage, size },
  })

export const getCommentRepliesApi = (rootId: number, currentPage = 1, size = 5) =>
  request.get<Result<Page<CommentVO>>>(`/comments/${rootId}/replies`, {
    params: { currentPage, size },
  })

/**
 * 删除评论
 */
export const deleteCommentByIdApi = (id: number) => {
  return request.delete<Result<boolean>>(`/comments/delete/${id}`)
}

export interface CommentLikeDto {
  commentId: number
}

/**
 * 评论点赞/取消点赞
 */
export function likeCommentApi(data: CommentLikeDto) {
  return request.put<Result<boolean>>('/comments/like', data)
}
