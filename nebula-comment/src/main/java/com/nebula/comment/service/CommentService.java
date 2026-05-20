package com.nebula.comment.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.nebula.comment.entity.Comment;

import java.util.List;

/**
 * 评论 Service
 */
public interface CommentService extends IService<Comment> {

    /**
     * 分页查询评论
     */
    Page<Comment> pageComments(int page, int size, Long articleId);

    /**
     * 查询评论详情
     */
    Comment getCommentById(Long id);

    /**
     * 批量删除评论
     */
    boolean deleteCommentsBatch(List<Long> ids);

    /**
     * 更新评论状态
     */
    boolean updateStatusBatch(List<Long> ids, Integer status);
}