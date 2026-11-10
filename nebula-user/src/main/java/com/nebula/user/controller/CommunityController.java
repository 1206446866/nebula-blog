package com.nebula.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "关注与粉丝", description = "用户发现、关注关系和粉丝列表；客户端社区入口目前隐藏。")
public class CommunityController {
    private final UserFollowService follows;

    @Operation(summary = "分页发现用户",
            description = "需已登录，按关键词筛选可展示用户。")
    @GetMapping("/users")
    public Result<Page<CommunityUserVO>> discover(@Parameter(description = "用户发现关键词，最多50字符") @RequestParam(required = false) @Size(max = 50) String keyword,
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") @Min(1) int current,
            @Parameter(description = "每页数量，以当前接口校验上限为准") @RequestParam(defaultValue = "12") @Min(1) @Max(100) int size) {
        return Result.success(follows.discover(keyword, current, size));
    }

    @Operation(summary = "查询用户关注关系摘要",
            description = "需已登录，返回关注、粉丝数量及当前账号与目标用户的关系。")
    @GetMapping("/users/{id}/relationship")
    public Result<FollowSummaryVO> summary(@Parameter(description = "目标用户编号") @PathVariable @Min(1) Long id) {
        return Result.success(follows.summary(id));
    }

    @Operation(summary = "关注用户",
            description = "需已登录，幂等设置为已关注，并检查目标账号和禁止关注本人的规则。")
    @PutMapping("/users/{id}/following")
    public Result<FollowSummaryVO> follow(@Parameter(description = "目标用户编号") @PathVariable @Min(1) Long id) {
        return Result.success(follows.setFollowing(id, true));
    }

    @Operation(summary = "取消关注用户",
            description = "需已登录，幂等设置为未关注，返回最新关系摘要。")
    @DeleteMapping("/users/{id}/following")
    public Result<FollowSummaryVO> unfollow(@Parameter(description = "目标用户编号") @PathVariable @Min(1) Long id) {
        return Result.success(follows.setFollowing(id, false));
    }

    @Operation(summary = "分页查询用户关注列表",
            description = "需已登录，返回目标用户关注的可展示账号。")
    @GetMapping("/users/{id}/following")
    public Result<Page<CommunityUserVO>> following(@Parameter(description = "目标用户编号") @PathVariable @Min(1) Long id,
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") @Min(1) int current,
            @Parameter(description = "每页数量，以当前接口校验上限为准") @RequestParam(defaultValue = "12") @Min(1) @Max(100) int size) {
        return Result.success(follows.connections(id, false, current, size));
    }

    @Operation(summary = "分页查询用户粉丝列表",
            description = "需已登录，返回关注目标用户的可展示账号。")
    @GetMapping("/users/{id}/followers")
    public Result<Page<CommunityUserVO>> followers(@Parameter(description = "目标用户编号") @PathVariable @Min(1) Long id,
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") @Min(1) int current,
            @Parameter(description = "每页数量，以当前接口校验上限为准") @RequestParam(defaultValue = "12") @Min(1) @Max(100) int size) {
        return Result.success(follows.connections(id, true, current, size));
    }
}
