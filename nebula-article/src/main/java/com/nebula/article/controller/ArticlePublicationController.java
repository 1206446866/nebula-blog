package com.nebula.article.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.nebula.article.dto.SchedulePublicationDTO;
import com.nebula.article.service.ArticlePublicationService;
import com.nebula.common.result.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/articles/{id}/publication")
@PreAuthorize("@securityUtils.isAdmin() && @securityUtils.hasPermission('article:update')")
@Tag(name = "文章定时发布", description = "管理员设置和取消文章定时发布计划。")
public class ArticlePublicationController {
    private final ArticlePublicationService publications;

    @Operation(summary = "设置文章定时发布",
            description = "需管理员及article:update权限，检查目标文章和未来发布时间，返回已设置的发布时间。")
    @PutMapping
    public Result<LocalDateTime> schedule(@Parameter(description = "文章编号") @PathVariable @Min(1) Long id, @RequestBody @Valid SchedulePublicationDTO dto) {
        return Result.success(publications.schedule(id, dto.getPublishAt()));
    }

    @Operation(summary = "取消文章定时发布",
            description = "需管理员及article:update权限，取消目标文章的发布计划。")
    @DeleteMapping
    public Result<Boolean> cancel(@Parameter(description = "文章编号") @PathVariable @Min(1) Long id) {
        publications.cancel(id);
        return Result.success(true);
    }
}
