package com.nebula.category.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.nebula.category.entity.Category;
import com.nebula.category.service.CategoryService;
import org.springframework.stereotype.Service;
import com.nebula.category.mapper.CategoryMapper;
import java.util.List;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Override
    public Page<Category> pageCategories(int page, int size, String name, String orderBy, boolean asc) {
        // TODO QW分页查询
        return null;
    }

    @Override
    public boolean deleteCategoriesBatch(List<Long> ids) {
        // TODO QW逻辑删除
        return false;
    }

    @Override
    public boolean updateStatusBatch(List<Long> ids, Integer status) {
        // TODO QW批量更新状态
        return false;
    }

    @Override
    public boolean updateCategoryPartial(Long id, Category category) {
        // TODO QW部分更新
        return false;
    }

    @Override
    public Category getCategoryById(Long id) {
        // TODO QW单条查询
        return null;
    }
}