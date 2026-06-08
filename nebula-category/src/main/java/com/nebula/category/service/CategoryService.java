package com.nebula.category.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.nebula.category.dto.CreateCategoryDTO;
import com.nebula.category.dto.UpdateCategoryDTO;
import com.nebula.category.entity.Category;
import com.nebula.category.vo.CategoryVO;

import java.util.List;

public interface CategoryService extends IService<Category> {

    Page<CategoryVO> pageCategories(String name, int page, int size, String orderBy, boolean asc);

    List<CategoryVO> listCategories();

    boolean createCategory(CreateCategoryDTO dto);

    boolean updateCategory(UpdateCategoryDTO dto);

    boolean deleteCategory(Long id);

    Category getCategoryById(Long id);
}