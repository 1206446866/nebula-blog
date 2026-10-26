package com.nebula.user.controller;

import com.mybatisflex.core.paginate.Page;
import com.nebula.common.result.Result;
import com.nebula.user.service.UserFollowService;
import com.nebula.user.vo.*;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/community")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class CommunityController {
    private final UserFollowService follows;

    @GetMapping("/users")
    public Result<Page<CommunityUserVO>> discover(@RequestParam(required = false) @Size(max = 50) String keyword,
            @RequestParam(defaultValue = "1") @Min(1) int current,
            @RequestParam(defaultValue = "12") @Min(1) @Max(100) int size) {
        return Result.success(follows.discover(keyword, current, size));
    }

    @GetMapping("/users/{id}/relationship")
    public Result<FollowSummaryVO> summary(@PathVariable @Min(1) Long id) {
        return Result.success(follows.summary(id));
    }

    @PutMapping("/users/{id}/following")
    public Result<FollowSummaryVO> follow(@PathVariable @Min(1) Long id) {
        return Result.success(follows.setFollowing(id, true));
    }

    @DeleteMapping("/users/{id}/following")
    public Result<FollowSummaryVO> unfollow(@PathVariable @Min(1) Long id) {
        return Result.success(follows.setFollowing(id, false));
    }

    @GetMapping("/users/{id}/following")
    public Result<Page<CommunityUserVO>> following(@PathVariable @Min(1) Long id,
            @RequestParam(defaultValue = "1") @Min(1) int current,
            @RequestParam(defaultValue = "12") @Min(1) @Max(100) int size) {
        return Result.success(follows.connections(id, false, current, size));
    }

    @GetMapping("/users/{id}/followers")
    public Result<Page<CommunityUserVO>> followers(@PathVariable @Min(1) Long id,
            @RequestParam(defaultValue = "1") @Min(1) int current,
            @RequestParam(defaultValue = "12") @Min(1) @Max(100) int size) {
        return Result.success(follows.connections(id, true, current, size));
    }
}
