package com.nebula.article.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.nebula.article.dto.*;
import com.nebula.article.entity.Article;
import com.nebula.article.vo.ArticleVO;

import java.util.List;

/** 文章查询、写作、发布及互动服务。 */
public interface ArticleService extends IService<Article> {

    /**
     * 分页查询当前用户有权查看的文章。
     *
     * @param dto 分页与筛选条件
     * @param orderBy 排序字段，可为空
     * @param asc 是否升序排列
     * @return 文章分页结果
     */
    Page<ArticleVO> pageArticles(ArticlePageDTO dto, String orderBy, boolean asc);


    /**
     * 读取有权查看的文章详情，已发布文章按规则记录浏览量。
     *
     * @param id 文章ID
     * @return 文章详情及当前用户的点赞状态
     */
    ArticleVO getArticleById(Long id);


    /**
     * 创建当前用户的私人草稿，保留旧接口的成功标记返回方式。
     *
     * @param dto 标题、正文、分类和标签
     * @return 是否创建成功
     */
    boolean createArticle(CreateArticleDto dto);

    /**
     * 创建当前用户的私人草稿并返回新文章ID。
     *
     * @param dto 标题、正文、分类和标签
     * @return 新草稿ID
     */
    Long createDraft(CreateArticleDto dto);

    /**
     * 读取本人或有权管理的文章用于编辑，不增加浏览量。
     *
     * @param id 文章ID
     * @return 包含分类和标签ID的编辑数据
     */
    ArticleVO getArticleForEdit(Long id);

    /**
     * 保存本人或有权管理的文章，保留作者和发布状态，同时取消原定时发布计划。
     *
     * @param dto 文章ID及待保存内容
     * @return 是否保存成功
     */
    boolean updateArticle(UpdateArticleDto dto);
    /**
     * 管理员发布或撤回文章，同时校验文章修改权限并取消原定时计划。
     *
     * @param dto 文章ID与目标状态：0草稿、1已发布
     * @return 是否修改成功
     */
    boolean changeArticleStatus(ChangeArticleStatusDto dto);

    /**
     * 按分类、标签等条件分页查询已发布且未删除的文章。
     *
     * @param dto 分页与筛选条件
     * @return 已发布文章分页结果
     */
    Page<ArticleVO> pagePublishedArticles(ArticlePageDTO dto);

    /**
     * 统计用户未删除的文章数量，包含草稿。
     *
     * @param userId 用户ID
     * @return 文章总数
     */
    Long getAllCount(Long userId);

    /**
     * 统计用户已发布文章的浏览总量。
     *
     * @param userId 用户ID
     * @return 浏览总量，没有记录时为0
     */
    Long getViewAllCount(Long userId);

    /**
     * 统计用户已发布文章的获赞总数。
     *
     * @param userId 用户ID
     * @return 获赞总数，没有记录时为0
     */
    Long getLikeAllCount(Long userId);

    /**
     * 分页查询指定用户的文章，按时间和ID倒序排列。
     *
     * @param userId 用户ID
     * @param status 发布状态，可为空
     * @param currentPage 页码，从1开始
     * @param pageSize 每页条数
     * @return 用户文章分页结果
     */
    Page<Article> pageArticleProfile(Long userId,Integer status, int currentPage, int pageSize);

    /**
     * 根据评论关联的文章ID批量查询已发布文章的标题。
     *
     * @param list 评论关联的文章ID列表，空列表不查询
     * @return 仅含ID和标题的文章列表
     */
    List<Article> getArticleTitlesByCommentIds(List<Long> list);

    /**
     * 切换当前用户对已发布文章的点赞，不能代替他人操作。
     *
     * @param articleLikeDto 待点赞或取消点赞的文章
     * @return true表示已点赞，false表示已取消
     */
    Boolean like(ArticleLikeDto articleLikeDto);

    /**
     * 删除本人或有权管理的文章，提交后清理详情缓存。
     *
     * @param id 文章ID
     * @return 是否删除成功
     */
    Boolean delete(Long id);
}
