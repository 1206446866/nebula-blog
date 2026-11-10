package com.nebula.category.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "分类管理", description = "分类查询及管理员维护。")
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "分页查询分类",
            description = "需category:list权限，支持名称筛选。")
    @PreAuthorize("hasAuthority('category:list')")
    @GetMapping
    public Result<Page<CategoryVO>> pageCategories(@Parameter(description = "名称筛选条件") @RequestParam(required = false) String name,
                                                   @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") @Min(1) int currentPage,
                                                   @Parameter(description = "每页数量，以当前接口校验上限为准") @Min(1) @Max(100) @RequestParam(defaultValue = "10") int size){
        return Result.success(categoryService.pageCategories(name,currentPage,size,null,false));
    }

    @Operation(summary = "查询分类列表",
            description = "需category:list权限，返回分类列表。")
    @PreAuthorize("hasAuthority('category:list')")
    @GetMapping("/list")
    public Result<List<CategoryVO>> listCategories(){
        return Result.success(categoryService.listCategories());
    }

    @Operation(summary = "创建分类",
            description = "需管理员及category:create权限，检查分类业务规则。")
    @PreAuthorize("hasAuthority('category:create') && hasRole('ADMIN')")
    @PostMapping("/create")
    public Result<Boolean> createCategory(@RequestBody @Valid CreateCategoryDTO dto){
        return Result.success(categoryService.createCategory(dto));
    }

    @Operation(summary = "修改分类",
            description = "需管理员及category:update权限，检查目标分类。")
    @PreAuthorize("hasAuthority('category:update') && hasRole('ADMIN')")
    @PutMapping("/edit")
    public Result<Boolean> updateCategory(@RequestBody @Valid UpdateCategoryDTO dto){
        return Result.success(categoryService.updateCategory(dto));
    }

    @Operation(summary = "删除分类",
            description = "需管理员及category:delete权限，按分类关联规则执行删除。")
    @PreAuthorize("hasAuthority('category:delete') && hasRole('ADMIN')")
    @DeleteMapping("/delete/{id}")
    public Result<Boolean> deleteCategory(@Parameter(description = "分类编号") @PathVariable @Min(1) Long id){
        return Result.success(categoryService.deleteCategory(id));
    }

}
