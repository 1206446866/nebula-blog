package com.nebula.article.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.nebula.article.dto.ChangeArticleStatusDto;
import com.nebula.article.entity.Article;

import java.util.List;

public interface ArticleService extends IService<Article> {

    /**
     * 分页查询文章
     *
     * @param page    当前页
     * @param size    每页大小
     * @param title   标题模糊搜索，可空
     * @param author  作者精确搜索，可空
     * @param orderBy 排序字段 createTime/updateTime，可空
     * @param asc     升序/降序
     * @return 分页数据
     */
    Page<Article> pageArticles(int page, int size, String title, String author, String orderBy, boolean asc);

    /**
     * 批量逻辑删除文章
     *
     * @param ids 文章 id 列表
     * @return 是否成功
     */
    boolean deleteArticlesBatch(List<Long> ids);

    /**
     * 批量更新文章状态
     *
     * @param ids    文章 id 列表
     * @param status 状态值（0删除，1正常）
     * @return 是否成功
     */
    boolean updateStatusBatch(List<Long> ids, Integer status);

    /**
     * 部分字段更新文章
     *
     * @param id      文章 id
     * @param article 只包含要更新的字段
     * @return 是否成功
     */
    boolean updateArticlePartial(Long id, Article article);

    /**
     * 根据 ID 查询文章
     *
     * @param id 文章 id
     * @return 文章对象
     */
    Article getArticleById(Long id);

    /**
     * 修改文章状态
     *
     * @param dto 修改文章状态请求参数
     * @return 是否修改成功
     */
    boolean changeArticleStatus(ChangeArticleStatusDto dto);
}
