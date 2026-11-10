package com.nebula.article.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.nebula.article.dto.WorkspaceDto;
import com.nebula.article.service.ArticleWorkspaceService;
import com.nebula.article.vo.WorkspaceVO;
import com.nebula.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/article-workspaces")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "私人编辑备份", description = "当前账号的云端编辑备份，与正式文章和发布计划独立。")
public class ArticleWorkspaceController {
    private final ArticleWorkspaceService workspaces;

    @Operation(summary = "读取本人编辑备份",
            description = "需已登录，已有文章仍需编辑权限；备份过期后仅返回版本号。")
    @GetMapping("/{key}")
    public Result<WorkspaceVO> get(@Parameter(description = "备份键：new或article-文章编号") @PathVariable String key) {
        return Result.success(workspaces.get(key));
    }

    @Operation(summary = "保存或清除本人编辑备份",
            description = "需已登录，按预期版本保存；内容为null时清除备份，并发修改时拒绝覆盖，不改变正式文章和发布计划。")
    @PutMapping("/{key}")
    public Result<WorkspaceVO> save(@Parameter(description = "备份键：new或article-文章编号") @PathVariable String key, @RequestBody @Valid WorkspaceDto dto) {
        return Result.success(workspaces.save(key, dto));
    }
}
