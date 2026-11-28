package com.nebula.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.nebula.api.dto.ProfileQueryDto;
import com.nebula.api.facade.TagHomeFacade;
import com.nebula.api.facade.UserProfileFacade;
import com.nebula.api.vo.tag.home.TagHomeVO;
import com.nebula.common.result.Result;
import com.nebula.common.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "客户端聚合信息", description = "用户主页和标签主页的聚合读取。")
public class ApiController {

    private final UserProfileFacade userProfileFacade;
    private final TagHomeFacade tagHomeFacade;

    @Operation(summary = "获取用户主页信息",
            description = "游客只读取正常账号的公开资料、已发布文章及公开评论。私人草稿及登录记录仅对本人展示。")
    @GetMapping("/profile/{id}")
    public Result<?> getProfile(@Parameter(description = "用户编号") @PathVariable("id") @Min(value = 1, message = "非法的用户ID") Long id, @Valid ProfileQueryDto dto) {
        return Result.success(SecurityUtils.getLoginUser() == null
                ? userProfileFacade.getPublicProfile(id, dto) : userProfileFacade.getProfile(id, dto));
    }

    @Operation(summary = "获取标签主页信息",
            description = "游客可读。登录后需tag:list权限，统计只计入已发布且未删除的文章。")
    @GetMapping("/tag")
    @PreAuthorize("isAnonymous() || @securityUtils.hasPermission('tag:list')")
    public Result<List<TagHomeVO>> getTagHome() {
        return Result.success(tagHomeFacade.getTagHome());
    }

}
