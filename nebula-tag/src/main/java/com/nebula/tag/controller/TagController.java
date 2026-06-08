package com.nebula.tag.controller;

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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @GetMapping
    public Result<Page<TagVO>> pageTags(@RequestParam(required = false) String name, @RequestParam(defaultValue = "1") @Min(1) int currentPage, @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return Result.success(tagService.pageTags(name, size, currentPage, null, false));
    }

    @GetMapping("list")
    public Result<List<TagVO>> listTags() {
        return Result.success(tagService.listTags());
    }

    @PostMapping("/create")
    public Result<Boolean> createTag(@RequestBody @Valid CreateTagDTO dto) {
        return Result.success(tagService.createTag(dto));
    }

    @PutMapping("/edit")
    public Result<Boolean> updateTag(@RequestBody @Valid UpdateTagDTO dto) {
        return Result.success(tagService.updateTag(dto));
    }

    @DeleteMapping("/delete/{id}")
    public Result<Boolean> deleteTag(@PathVariable @Min(1) Long id) {
        return Result.success(tagService.deleteTag(id));
    }

}