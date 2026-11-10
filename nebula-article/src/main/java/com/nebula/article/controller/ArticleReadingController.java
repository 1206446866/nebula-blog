package com.nebula.article.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.nebula.article.dto.ReadingDto;
import com.nebula.article.service.ArticleReadingService;
import com.nebula.article.vo.ReadingVO;
import com.nebula.common.result.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reading")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Validated
@Tag(name = "私人阅读书架", description = "当前账号的收藏、稍后读及阅读历史。")
public class ArticleReadingController {
    private final ArticleReadingService reading;
    @Operation(summary = "查询本人阅读状态",
            description = "需已登录且目标文章仍可阅读；返回收藏、稍后读和阅读位置，本次查询不增加历史。")
    @GetMapping("/{id}")
    public Result<ReadingVO.State> get(@Parameter(description = "文章编号") @PathVariable @Positive long id) { return Result.success(reading.get(id)); }
    @Operation(summary = "设置收藏与稍后读",
            description = "需已登录，幂等设置当前账号的目标标记，只接受当前可读的已发布文章。")
    @PutMapping("/{id}/flags")
    public Result<ReadingVO.State> flags(@Parameter(description = "文章编号") @PathVariable @Positive long id, @RequestBody @Valid ReadingDto.Flags flags) {
        return Result.success(reading.flags(id, flags));
    }
    @Operation(summary = "保存本人阅读位置",
            description = "需已登录，只为当前仍可读的已发布文章保存本次阅读进度。")
    @PutMapping("/{id}/progress")
    public Result<ReadingVO.State> progress(@Parameter(description = "文章编号") @PathVariable @Positive long id, @RequestBody @Valid ReadingDto.Progress progress) {
        return Result.success(reading.progress(id, progress));
    }
    @Operation(summary = "分页查询本人书架",
            description = "需已登录，按收藏、稍后读或历史查询，过滤已撤回和删除的文章。")
    @GetMapping
    public Result<ReadingVO.Library> library(@Parameter(description = "书架类型：favorite、later或history") @RequestParam(defaultValue = "favorite") String kind,
                                             @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") @Min(1) @Max(10000) int current,
                                             @Parameter(description = "每页数量，以当前接口校验上限为准") @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return Result.success(reading.library(kind, current, size));
    }
    @Operation(summary = "清除本人阅读历史",
            description = "需已登录，仅清除历史，保留收藏及稍后读标记。")
    @DeleteMapping("/history")
    public Result<Boolean> clear() { return Result.success(reading.clearHistory()); }
}
