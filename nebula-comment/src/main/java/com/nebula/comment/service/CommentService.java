package com.nebula.comment.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.nebula.comment.dto.ReleaseCommentDto;
import com.nebula.comment.entity.Comment;
import com.nebula.comment.vo.CommentVO;

import java.util.List;

/**
 * 评论 Service
 */
public interface CommentService extends IService<Comment> {

    /**
     * 分页查询评论
     */
    Page<CommentVO> pageComments(Long articleId,String content,int page, int size);


    /**
     * 批量删除评论
     */
    boolean deleteCommentsBatch(List<Long> ids);


    /**
     * 发布评论
     *
     * @param dto 评论发布请求
     * @return 是否发布成功
     */
    Boolean releaseComment(ReleaseCommentDto dto);

    /**
     * 删除评论
     * <p>
     * 删除指定评论。
     * 管理员可以删除任意评论，
     * 普通用户仅允许删除自己发布的评论。
     *
     * @param id 评论ID
     * @return 是否删除成功
     */
    Boolean deleteCommentById(Long id);
}