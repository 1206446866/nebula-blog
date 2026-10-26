package com.nebula.tag.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.nebula.tag.dto.CreateTagDTO;
import com.nebula.tag.dto.UpdateTagDTO;
import com.nebula.tag.entity.Tag;
import com.nebula.tag.vo.TagVO;

import java.util.List;

/** 文章标签管理服务。 */
public interface TagService extends IService<Tag> {

    /**
     * 按名称筛选并分页查询未删除的标签。
     *
     * @param name 标签名称，可为空
     * @param page 页码，从1开始
     * @param size 每页条数
     * @param orderBy 排序字段，可为空
     * @param asc 是否升序排列
     * @return 标签分页结果
     */
    Page<TagVO> pageTags(String name, int page, int size, String orderBy, boolean asc);

    /**
     * 查询全部未删除标签，供文章标签选择使用。
     *
     * @return 标签列表
     */
    List<TagVO> listTags();

    /**
     * 创建标签。
     *
     * @param dto 标签名称与说明
     * @return 是否创建成功
     */
    boolean createTag(CreateTagDTO dto);

    /**
     * 修改仍存在的标签。
     *
     * @param dto 标签ID、名称与说明
     * @return 是否修改成功
     */
    boolean updateTag(UpdateTagDTO dto);

    /**
     * 删除未被有效文章使用的标签，并清理其关联记录。
     *
     * @param id 标签ID
     * @return 是否删除成功
     */
    boolean deleteTag(Long id);

}