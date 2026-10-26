package com.nebula.comment.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.nebula.comment.dto.CommentLikeDto;
import com.nebula.comment.dto.ReleaseCommentDto;
import com.nebula.comment.entity.Comment;
import com.nebula.comment.vo.CommentVO;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

/** 文章评论、讨论回复及点赞服务。 */
public interface CommentService extends IService<Comment> {

    /**
     * 在文章可见范围内分页查询评论及当前用户点赞状态。
     *
     * @param articleId 文章ID，可为空
     * @param content 评论内容关键词，可为空
     * @param page 页码，从1开始
     * @param size 每页条数
     * @return 评论分页结果
     */
    Page<CommentVO> pageComments(Long articleId,String content,int page, int size);

    /**
     * 分页查询主评论，并附带每条讨论的回复数量。
     *
     * @param articleId 当前用户有权查看的文章ID
     * @param page 页码，从1开始
     * @param size 每页主评论条数
     * @return 主评论分页结果
     */
    Page<CommentVO> pageThreads(Long articleId, int page, int size);

    /**
     * 分页查询一条主评论下的所有层级回复，按时间正序排列。
     *
     * @param rootId 主评论ID
     * @param page 页码，从1开始
     * @param size 每页回复条数
     * @return 带实际回复目标的回复分页结果
     */
    Page<CommentVO> pageReplies(Long rootId, int page, int size);


    /**
     * 以当前登录用户身份发表评论或回复，拒绝跨文章及无效回复目标。
     *
     * @param dto 文章、评论内容及可选的回复目标
     * @return 是否发表成功
     */
    Boolean releaseComment(ReleaseCommentDto dto);

    /**
     * 校验本人或管理员权限后，删除评论及其全部后续回复和点赞关联。
     *
     * @param id 待删除评论ID
     * @return 是否删除成功
     */
    Boolean deleteCommentById(Long id);

    /**
     * 统计用户未删除的评论数量。
     *
     * @param userId 用户ID
     * @return 评论总数
     */
    Long getAllCount(Long userId);

    /**
     * 分页查询用户在已发布、未删除文章上的评论。
     *
     * @param userId 用户ID
     * @param currentPage 页码，从1开始
     * @param size 每页条数
     * @return 公开评论分页结果
     */
    Page<Comment> getCommentPageByUserId(Long userId, int currentPage,  int size);

    /**
     * 批量统计当前可读文章的评论数量，空列表不查询。
     *
     * @param list 文章ID列表
     * @return 文章ID与评论数的映射
     */
    Map<Long,Long> getCountByArticleIds(List<Long> list);

    /**
     * 切换当前用户对可读评论的点赞，点赞关联与计数一同更新。
     *
     * @param dto 待点赞或取消点赞的评论
     * @return true表示已点赞，false表示已取消
     */
    Boolean like(@Valid CommentLikeDto dto);

    /**
     * 统计用户在已发布、未删除文章上的评论获赞总数。
     *
     * @param userId 用户ID
     * @return 获赞总数，没有记录时为0
     */
    Long getLikeAllCount(Long userId);
}
