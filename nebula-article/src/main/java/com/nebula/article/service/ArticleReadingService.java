package com.nebula.article.service;

import com.nebula.article.dto.ReadingDto;
import com.nebula.article.vo.ReadingVO;

/** 管理当前账号的收藏、稍后读和阅读历史，统一过滤不可读文章。 */
public interface ArticleReadingService {
    /**
     * 查询本人对已发布文章的阅读状态，不增加阅读历史。
     * @param articleId 已发布文章编号
     * @return 收藏、稍后读及阅读位置
     */
    ReadingVO.State get(long articleId);
    /**
     * 幂等设置本人的收藏与稍后读标记。
     * @param articleId 已发布文章编号
     * @param flags 目标标记
     * @return 更新后的私人状态
     */
    ReadingVO.State flags(long articleId, ReadingDto.Flags flags);
    /**
     * 保存本人本次阅读位置；只接受当前仍可读的已发布文章。
     * @param articleId 文章编号
     * @param progress 本次位置
     * @return 保存后的私人状态
     */
    ReadingVO.State progress(long articleId, ReadingDto.Progress progress);
    /**
     * 分页读取本人的收藏、稍后读或历史，过滤已撤回及删除文章。
     * @param kind favorite、later 或 history
     * @param current 页码
     * @param size 每页数量，最多50条
     * @return 私人阅读列表
     */
    ReadingVO.Library library(String kind, int current, int size);
    /**
     * 清除本人阅读历史，保留收藏及稍后读。
     * @return 是否完成
     */
    boolean clearHistory();
}
