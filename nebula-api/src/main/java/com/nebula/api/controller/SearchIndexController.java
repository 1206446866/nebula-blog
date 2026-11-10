package com.nebula.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.nebula.api.search.SearchIndexService;
import com.nebula.common.result.Result;
import com.nebula.common.util.SecurityUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/search/index")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "搜索索引维护", description = "仅超级管理员查看同步状态和申请重建，不提供任意ES查询或索引名入口。")
public class SearchIndexController {
    private final SearchIndexService index;
    public SearchIndexController(SearchIndexService index) { this.index = index; }
    @Operation(summary = "查询搜索索引同步状态",
            description = "仅超级管理员可操作，返回就绪状态、待同步数量和任务错误分类。")
    @GetMapping("/status")
    public Result<Map<String, Object>> status() {
        SecurityUtils.requireSuperAdmin(); return Result.success(index.status());
    }
    @Operation(summary = "申请重建搜索索引",
            description = "仅超级管理员可操作，异步构建新索引并在完成后切换，返回当前同步状态。")
    @PostMapping("/rebuild")
    public Result<Map<String, Object>> rebuild() {
        SecurityUtils.requireSuperAdmin(); index.requestRebuild(); return Result.success(index.status());
    }
}
