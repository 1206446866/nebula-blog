package com.nebula.article.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.nebula.article.dto.ChangeArticleStatusDto;
import com.nebula.article.dto.CreateArticleDto;
import com.nebula.article.dto.UpdateArticleDto;
import com.nebula.article.entity.Article;
import com.nebula.article.vo.ArticleVO;
import org.apache.ibatis.javassist.NotFoundException;

public interface ArticleService extends IService<Article> {

    /**
     * 分页查询文章
     *
     * @param page    当前页
     * @param size    每页大小
     * @param userId  用户ID
     * @param title   标题模糊搜索，可空
     * @param author  作者精确搜索，可空
     * @param orderBy 排序字段 createTime/updateTime，可空
     * @param asc     升序/降序
     * @return 分页数据
     */
    Page<ArticleVO> pageArticles(int page, int size, Long userId, String title, String author, String orderBy, boolean asc);


    /**
     * 根据 ID 查询文章
     *
     * @param id 文章 id
     * @return 文章对象
     */
    ArticleVO getArticleById(Long id) throws NotFoundException;


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
     * @param currentPage 当前页
     * @param size        每页条数
     * @param title       标题
     * @return 已发布文章分页数据
     */
    Page<ArticleVO> pagePublishedArticles(String title,Integer currentPage, Integer size);
}
