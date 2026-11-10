package com.nebula.article.service;

import com.nebula.article.entity.Article;
import com.nebula.article.vo.ArticleRevisionVO;
import java.util.List;

/** 保存手动编辑的历史快照，仅向当前有权编辑文章的用户提供恢复内容。 */
public interface ArticleRevisionService {
    /**
     * 为手动保存的正文建立快照，同一编辑版本只保留一份，最多保存最近30份。
     * @param article 已保存的文章；调用方需在授权后的写事务中持有文章锁
     */
    void capture(Article article);
    /**
     * 查询本人或管理员有权编辑的文章历史。
     * @param articleId 文章编号
     * @return 最近30份版本摘要，按新到旧排列
     */
    List<ArticleRevisionVO.Summary> list(long articleId);
    /**
     * 读取历史内容供编辑器恢复，读取本身不会更新或发布文章。
     * @param articleId 有权编辑的文章编号
     * @param revisionId 该文章的版本编号
     * @return 原标题、正文及目录关联
     */
    ArticleRevisionVO.Detail get(long articleId, long revisionId);
}
