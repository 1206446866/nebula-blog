package com.nebula.category.controller;

import com.mybatisflex.core.paginate.Page;
import com.nebula.category.dto.CreateCategoryDTO;
import com.nebula.category.dto.UpdateCategoryDTO;
import com.nebula.category.service.CategoryService;
import com.nebula.category.vo.CategoryVO;
import com.nebula.common.result.Result;
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
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public Result<Page<CategoryVO>> pageCategories(@RequestParam(required = false) String name,
                                                   @RequestParam(defaultValue = "1") @Min(1) int currentPage,
                                                   @Min(1) @Max(100) @RequestParam(defaultValue = "10") int size){
        return Result.success(categoryService.pageCategories(name,currentPage,size,null,false));
    }

    @PreAuthorize("hasAuthority('category:list')")
    @GetMapping("/list")
    public Result<List<CategoryVO>> listCategories(){
        return Result.success(categoryService.listCategories());
    }

    @PreAuthorize("hasAuthority('category:create')")
    @PostMapping("/create")
    public Result<Boolean> createCategory(@RequestBody @Valid CreateCategoryDTO dto){
        return Result.success(categoryService.createCategory(dto));
    }

    @PreAuthorize("hasAuthority('category:update')")
    @PutMapping("/edit")
    public Result<Boolean> updateCategory(@RequestBody @Valid UpdateCategoryDTO dto){
        return Result.success(categoryService.updateCategory(dto));
    }

    @PreAuthorize("hasAuthority('category:delete')")
    @DeleteMapping("/delete/{id}")
    public Result<Boolean> deleteCategory(@PathVariable @Min(1) Long id){
        return Result.success(categoryService.deleteCategory(id));
    }

}
