package com.nebula.category.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.nebula.category.dto.CreateCategoryDTO;
import com.nebula.category.dto.UpdateCategoryDTO;
import com.nebula.category.entity.Category;
import com.nebula.category.vo.CategoryVO;

import java.util.List;

/** 文章分类管理服务。 */
public interface CategoryService extends IService<Category> {

    /**
     * 按名称筛选并分页查询未删除的分类。
     *
     * @param name 分类名称，可为空
     * @param page 页码，从1开始
     * @param size 每页条数
     * @param orderBy 排序字段，可为空
     * @param asc 是否升序排列
     * @return 分类分页结果
     */
    Page<CategoryVO> pageCategories(String name, int page, int size, String orderBy, boolean asc);

    /**
     * 查询全部未删除分类，供文章分类选择使用。
     *
     * @return 分类列表
     */
    List<CategoryVO> listCategories();

    /**
     * 创建分类。
     *
     * @param dto 分类名称与说明
     * @return 是否创建成功
     */
    boolean createCategory(CreateCategoryDTO dto);

    /**
     * 修改仍存在的分类。
     *
     * @param dto 分类ID、名称与说明
     * @return 是否修改成功
     */
    boolean updateCategory(UpdateCategoryDTO dto);

    /**
     * 删除未被有效文章使用的分类，并清理其关联记录。
     *
     * @param id 分类ID
     * @return 是否删除成功
     */
    boolean deleteCategory(Long id);

}