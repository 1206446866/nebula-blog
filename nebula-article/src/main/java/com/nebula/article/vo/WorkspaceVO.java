package com.nebula.article.vo;

import com.nebula.article.dto.WorkspaceDto;

/** 空备份仍返回版本号，防止清除后被旧页面重新写入。 */
public record WorkspaceVO(long revision, WorkspaceDto.Draft draft, String updatedAt) {}
