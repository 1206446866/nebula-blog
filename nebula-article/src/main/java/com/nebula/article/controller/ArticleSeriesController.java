package com.nebula.article.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.nebula.article.dto.ArticleSeriesDto;
import com.nebula.article.service.ArticleSeriesService;
import com.nebula.article.vo.ArticleSeriesVO;
import com.nebula.common.result.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/series")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Validated
@Tag(name = "文章系列", description = "维护有序文章系列并展示公开篇章。")
public class ArticleSeriesController {
    private final ArticleSeriesService series;
    @Operation(summary = "分页查询文章系列",
            description = "需已登录，默认查询公开系列，mine为true时查询本人系列；每页20条。")
    @GetMapping
    public Result<ArticleSeriesVO.Listing> list(@Parameter(description = "是否仅查询本人的系列") @RequestParam(defaultValue="false") boolean mine, @Parameter(description = "页码，从1开始") @RequestParam(defaultValue="1") @Min(1) @Max(10000) int current) { return Result.success(series.list(mine, current)); }
    @Operation(summary = "查询系列详情",
            description = "需已登录，普通读取仅展示有权查看的系列和已发布文章。")
    @GetMapping("/{id}")
    public Result<ArticleSeriesVO.Detail> get(@Parameter(description = "系列编号") @PathVariable @Positive long id) { return Result.success(series.get(id, false)); }
    @Operation(summary = "读取系列编辑内容",
            description = "需已登录，仅系列作者或管理员可以读取编辑内容。")
    @GetMapping("/{id}/edit")
    public Result<ArticleSeriesVO.Detail> edit(@Parameter(description = "系列编号") @PathVariable @Positive long id) { return Result.success(series.get(id, true)); }
    @Operation(summary = "创建文章系列",
            description = "需已登录及业务写作权限，只能关联作者自己的有效文章，返回系列编号。")
    @PostMapping
    public Result<Long> create(@RequestBody @Valid ArticleSeriesDto dto) { return Result.success(series.save(null, dto)); }
    @Operation(summary = "修改文章系列",
            description = "需已登录，仅系列作者或管理员可以修改，返回系列编号。")
    @PutMapping("/{id}")
    public Result<Long> update(@Parameter(description = "系列编号") @PathVariable @Positive long id, @RequestBody @Valid ArticleSeriesDto dto) { return Result.success(series.save(id, dto)); }
    @Operation(summary = "删除文章系列",
            description = "需已登录，仅系列作者或管理员可以删除；不删除系列中的文章。")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@Parameter(description = "系列编号") @PathVariable @Positive long id) { return Result.success(series.delete(id)); }
    @Operation(summary = "查询文章所属公开系列",
            description = "需已登录，为已发布文章返回最多20个所属公开系列。")
    @GetMapping("/for-article/{id}")
    public Result<List<ArticleSeriesVO.Summary>> forArticle(@Parameter(description = "文章编号") @PathVariable @Positive long id) { return Result.success(series.forArticle(id)); }
}
