package com.nebula.article.service;

import com.nebula.article.dto.ArticleSeriesDto;
import com.nebula.article.vo.ArticleSeriesVO;
import java.util.List;

/** 管理作者的有序文章系列，并向读者提供过滤草稿后的公开篇章。 */
public interface ArticleSeriesService {
    /**
     * 分页查询公开系列或本人全部系列，公开结果不计入草稿。
     * @param mine 是否仅查询本人
     * @param current 页码
     * @return 每页20条的系列摘要
     */
    ArticleSeriesVO.Listing list(boolean mine, int current);
    /**
     * 查询系列内容；编辑读取仅限作者或管理员，普通读取仅返回已发布文章。
     * @param id 系列编号
     * @param editing 是否读取编辑内容
     * @return 系列及有序文章
     */
    ArticleSeriesVO.Detail get(long id, boolean editing);
    /**
     * 创建或修改系列，只能关联系列作者自己的有效文章，默认可由客户端选择保持私有。
     * @param id null 表示创建，修改需作者或管理员权限
     * @param dto 标题、介绍、公开标记及有序文章编号，最多100篇
     * @return 系列编号
     */
    long save(Long id, ArticleSeriesDto dto);
    /**
     * 删除作者本人或管理员有权管理的系列，不删除文章。
     * @param id 系列编号
     * @return 是否删除成功
     */
    boolean delete(long id);
    /**
     * 查询已发布文章所属的公开系列，供文章详情串联阅读。
     * @param articleId 文章编号
     * @return 最多20个公开系列
     */
    List<ArticleSeriesVO.Summary> forArticle(long articleId);
}
