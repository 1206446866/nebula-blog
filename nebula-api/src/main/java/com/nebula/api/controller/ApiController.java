package com.nebula.api.controller;

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
public class ApiController {

    private final UserProfileFacade userProfileFacade;
    private final TagHomeFacade tagHomeFacade;

    /**
     * 获取用户主页信息
     *
     * @return 用户主页信息
     */
    @GetMapping("/profile/{id}")
    public Result<UserProfileVO> getProfile(@PathVariable("id") @Min(value = 1, message = "非法的用户ID") Long id, @Valid ProfileQueryDto dto) {
        UserProfileVO profile = userProfileFacade.getProfile(id, dto);
        return Result.success(profile);
    }

    /**
     * 获取标签主页信息
     *
     * @return 标签主页信息
     */
    @GetMapping("/tag")
    @PreAuthorize("@securityUtils.hasPermission('tag:list')")
    public Result<List<TagHomeVO>> getTagHome() {
        return Result.success(tagHomeFacade.getTagHome());
    }

}
