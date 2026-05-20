package com.nebula.category.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.nebula.category.entity.Category;

import java.util.List;

public interface CategoryService extends IService<Category> {

    Page<Category> pageCategories(int page, int size, String name, String orderBy, boolean asc);

    boolean deleteCategoriesBatch(List<Long> ids);

    boolean updateStatusBatch(List<Long> ids, Integer status);

    boolean updateCategoryPartial(Long id, Category category);

    Category getCategoryById(Long id);
}