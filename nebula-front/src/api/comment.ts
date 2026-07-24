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
}

/**
 * 发布评论DTO
 */
export interface ReleaseCommentDto {
  articleId: number
  content: string
}

export const getCommentPageApi = (
  articleId?: number,
  content?: string,
  currentPage = 1,
  size = 10,
) => {
  return request.get<Result<Page<CommentVO>>>('/comments', {
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
  return request.put('/comments/release', data)
}

/**
 * 删除评论
 */
export const deleteCommentByIdApi = (id: number) => {
  return request.delete(`/comments/delete/${id}`)
}

export interface CommentLikeDto {
  commentId: number
  userId: number
}

/**
 * 评论点赞/取消点赞
 */
export function likeCommentApi(data: CommentLikeDto) {
  return request.put('/comments/like', data)
}
