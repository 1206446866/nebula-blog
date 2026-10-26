package com.nebula.api.controller;

import com.nebula.api.service.GlobalSearchService;
import com.nebula.api.vo.search.SearchResultVO;
import com.nebula.common.result.Result;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/search")
@PreAuthorize("isAuthenticated()")
public class GlobalSearchController {
    private final GlobalSearchService search;

    @GetMapping
    public Result<SearchResultVO> search(@RequestParam(defaultValue = "") @Size(max = 50) String q,
            @RequestParam(defaultValue = "all") String type,
            @RequestParam(defaultValue = "1") @Min(1) int current,
            @RequestParam(defaultValue = "12") @Min(1) @Max(50) int size) {
        return Result.success(search.search(q, type, current, size));
    }
}
