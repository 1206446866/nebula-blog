package com.nebula.article.service;

import com.nebula.article.dto.WorkspaceDto;
import com.nebula.article.vo.WorkspaceVO;

/** 管理当前账号的私人编辑备份，不改动正式文章或发布计划。 */
public interface ArticleWorkspaceService {
    /**
     * 读取本人编辑备份；已有文章仍需具备编辑权限，备份过期后仅返回版本号。
     * @param key new 或 article-文章编号
     * @return 版本号、可恢复内容及保存时间
     */
    WorkspaceVO get(String key);

    /**
     * 按预期版本保存或清除本人备份，不改变正式文章及发布计划。
     * @param key new 或 article-文章编号
     * @param dto 预期版本与备份内容；内容为 null 时清除
     * @return 新版本及保存结果；并发修改时拒绝覆盖
     */
    WorkspaceVO save(String key, WorkspaceDto dto);
}
