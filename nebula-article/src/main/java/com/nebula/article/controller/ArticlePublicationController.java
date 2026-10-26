package com.nebula.article.controller;

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
public class ArticlePublicationController {
    private final ArticlePublicationService publications;

    @PutMapping
    public Result<LocalDateTime> schedule(@PathVariable @Min(1) Long id, @RequestBody @Valid SchedulePublicationDTO dto) {
        return Result.success(publications.schedule(id, dto.getPublishAt()));
    }

    @DeleteMapping
    public Result<Boolean> cancel(@PathVariable @Min(1) Long id) {
        publications.cancel(id);
        return Result.success(true);
    }
}
