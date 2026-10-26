package com.nebula.article.service;

import java.time.OffsetDateTime;
import java.time.LocalDateTime;

/** 文章定时发布计划管理与后台执行服务。 */
public interface ArticlePublicationService {
    /**
     * 为草稿创建或替换发布计划；仅管理员且具备文章修改权限可操作。
     * @param articleId 未删除草稿的ID
     * @param publishAt 带时区偏移的未来时间
     * @return 按上海时区保存的计划时间
     */
    LocalDateTime schedule(Long articleId, OffsetDateTime publishAt);

    /**
     * 取消草稿的定时计划并清除暂停原因，重复取消不影响正文。
     * @param articleId 文章ID，仅管理员且具备文章修改权限可操作
     */
    void cancel(Long articleId);

    /**
     * 后台处理最多100篇到期草稿，每篇独立事务并重新校验安排者权限。
     * @return 本批成功发布数量；无效计划暂停，临时失败留待下次重试
     */
    int publishDue();
}
