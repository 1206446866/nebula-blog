package com.nebula.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.nebula.api.dto.ProfileQueryDto;
import com.nebula.api.facade.TagHomeFacade;
import com.nebula.api.facade.UserProfileFacade;
import com.nebula.api.vo.profile.UserProfileVO;
import com.nebula.api.vo.tag.home.TagHomeVO;
import com.nebula.common.result.Result;
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
            description = "返回公开资料和分页内容，私人草稿及登录记录仅对本人展示。")
    @GetMapping("/profile/{id}")
    public Result<UserProfileVO> getProfile(@Parameter(description = "用户编号") @PathVariable("id") @Min(value = 1, message = "非法的用户ID") Long id, @Valid ProfileQueryDto dto) {
        UserProfileVO profile = userProfileFacade.getProfile(id, dto);
        return Result.success(profile);
    }

    @Operation(summary = "获取标签主页信息",
            description = "需tag:list权限，返回标签主页所需的聚合资料。")
    @GetMapping("/tag")
    @PreAuthorize("@securityUtils.hasPermission('tag:list')")
    public Result<List<TagHomeVO>> getTagHome() {
        return Result.success(tagHomeFacade.getTagHome());
    }

}
