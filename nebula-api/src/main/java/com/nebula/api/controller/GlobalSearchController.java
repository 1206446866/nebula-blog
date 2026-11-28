package com.nebula.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.nebula.api.service.GlobalSearchService;
import com.nebula.api.vo.search.SearchResultVO;
import com.nebula.common.result.Result;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/search")
@Tag(name = "全站搜索", description = "按当前权限搜索文章、评论、用户、分类和标签。")
public class GlobalSearchController {
    private final GlobalSearchService search;

    @Operation(summary = "全站搜索",
            description = "游客可搜索公开资料。登录后按权限限制搜索类型。关键词最多50字符，数量可能为估计值，最多访问前10000条结果。")
    @GetMapping
    public Result<SearchResultVO> search(@Parameter(description = "搜索关键词，最多50字符") @RequestParam(defaultValue = "") @Size(max = 50) String q,
            @Parameter(description = "搜索类型：all、article、comment、user、category或tag") @RequestParam(defaultValue = "all") String type,
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") @Min(1) int current,
            @Parameter(description = "每页数量，以当前接口校验上限为准") @RequestParam(defaultValue = "12") @Min(1) @Max(50) int size) {
        return Result.success(search.search(q, type, current, size));
    }
}
