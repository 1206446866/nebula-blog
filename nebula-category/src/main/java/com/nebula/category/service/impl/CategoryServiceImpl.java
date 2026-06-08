package com.nebula.category.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.util.StringUtil;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.nebula.category.dto.CreateCategoryDTO;
import com.nebula.category.dto.UpdateCategoryDTO;
import com.nebula.category.entity.Category;
import com.nebula.category.mapper.CategoryMapper;
import com.nebula.category.service.CategoryService;
import com.nebula.category.vo.CategoryVO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

import static com.nebula.category.entity.table.CategoryTableDef.CATEGORY;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Override
    public Page<CategoryVO> pageCategories(String name, int page, int size, String orderBy, boolean asc) {
        QueryWrapper queryWrapper = new QueryWrapper()
                .where(CATEGORY.NAME.like(name, StringUtil::hasText));
        if (Objects.nonNull(orderBy))
            queryWrapper.orderBy(orderBy, asc);
        return pageAs(Page.of(page, size), queryWrapper, CategoryVO.class);
    }

    @Override
    public List<CategoryVO> listCategories() {
        return listAs(QueryWrapper.create(),CategoryVO.class);
    }

    @Override
    public boolean createCategory(CreateCategoryDTO dto) {
        return save(Category.create().setName(dto.getName()).setDescription(dto.getDescription()));
    }

    @Override
    public boolean updateCategory(UpdateCategoryDTO dto) {
        return updateById(BeanUtil.copyProperties(dto, Category.class));
    }

    @Override
    public boolean deleteCategory(Long id) {
        return removeById(id);
    }

    @Override
    public Category getCategoryById(Long id) {
        // TODO QW单条查询
        return null;
    }
}