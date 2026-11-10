package com.nebula.tag.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.mybatisflex.core.paginate.Page;
import com.nebula.common.result.Result;
import com.nebula.tag.dto.CreateTagDTO;
import com.nebula.tag.dto.UpdateTagDTO;
import com.nebula.tag.service.TagService;
import com.nebula.tag.vo.TagVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/tags")
@RequiredArgsConstructor
@Tag(name = "标签管理", description = "标签查询及管理员维护。")
public class TagController {

    private final TagService tagService;

    @Operation(summary = "分页查询标签",
            description = "需tag:list权限，支持名称筛选。")
    @PreAuthorize("hasAuthority('tag:list')")
    @GetMapping
    public Result<Page<TagVO>> pageTags(@Parameter(description = "名称筛选条件") @RequestParam(required = false) String name, @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") @Min(1) int current, @Parameter(description = "每页数量，以当前接口校验上限为准") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return Result.success(tagService.pageTags(name, current, size, null, false));
    }

    @Operation(summary = "查询标签列表",
            description = "需tag:list权限，返回标签列表。")
    @PreAuthorize("hasAuthority('tag:list')")
    @GetMapping("list")
    public Result<List<TagVO>> listTags() {
        return Result.success(tagService.listTags());
    }

    @Operation(summary = "创建标签",
            description = "需管理员及tag:create权限，检查标签业务规则。")
    @PreAuthorize("hasAuthority('tag:create') && hasRole('ADMIN')")
    @PostMapping("/create")
    public Result<Boolean> createTag(@RequestBody @Valid CreateTagDTO dto) {
        return Result.success(tagService.createTag(dto));
    }

    @Operation(summary = "修改标签",
            description = "需管理员及tag:update权限，检查目标标签。")
    @PreAuthorize("hasAuthority('tag:update') && hasRole('ADMIN')")
    @PutMapping("/edit")
    public Result<Boolean> updateTag(@RequestBody @Valid UpdateTagDTO dto) {
        return Result.success(tagService.updateTag(dto));
    }

    @Operation(summary = "删除标签",
            description = "需管理员及tag:delete权限，按标签关联规则执行删除。")
    @PreAuthorize("hasAuthority('tag:delete') && hasRole('ADMIN')")
    @DeleteMapping("/delete/{id}")
    public Result<Boolean> deleteTag(@Parameter(description = "标签编号") @PathVariable @Min(1) Long id) {
        return Result.success(tagService.deleteTag(id));
    }

}