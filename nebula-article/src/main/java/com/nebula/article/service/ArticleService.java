package com.nebula.article.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.nebula.article.dto.*;
import com.nebula.article.entity.Article;
import com.nebula.article.vo.ArticleVO;

import java.util.List;

public interface ArticleService extends IService<Article> {

    /**
     * 分页查询文章
     *
     * @param orderBy 排序字段 createTime/updateTime，可空
     * @param asc     升序/降序
     * @return 分页数据
     */
    Page<ArticleVO> pageArticles(ArticlePageDTO dto, String orderBy, boolean asc);


    /**
     * 根据 ID 查询文章
     *
     * @param id 文章 id
     * @return 文章对象
     */
    ArticleVO getArticleById(Long id);


    boolean createArticle(CreateArticleDto dto);

    boolean updateArticle(UpdateArticleDto dto);
    /**
     * 修改文章状态
     *
     * @param dto 修改文章状态请求参数
     * @return 是否修改成功
     */
    boolean changeArticleStatus(ChangeArticleStatusDto dto);

    /**
     * 分页查询已发布文章
     *
     * @return 已发布文章分页数据
     */
    Page<ArticleVO> pagePublishedArticles(ArticlePageDTO dto);

    Long getAllCount(Long userId);

    Long getViewAllCount(Long userId);

    Long getLikeAllCount(Long userId);

    Page<Article> pageArticleProfile(Long userId,Integer status, int currentPage, int pageSize);

    List<Article> getArticleTitlesByCommentIds(List<Long> list);

    Boolean like(ArticleLikeDto articleLikeDto);

    Boolean delete(Long id);
}
