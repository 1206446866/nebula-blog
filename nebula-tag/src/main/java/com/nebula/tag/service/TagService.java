package com.nebula.tag.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.nebula.tag.dto.CreateTagDTO;
import com.nebula.tag.dto.UpdateTagDTO;
import com.nebula.tag.entity.Tag;
import com.nebula.tag.vo.TagVO;

import java.util.List;

public interface TagService extends IService<Tag> {

    Page<TagVO> pageTags(String name, int page, int size, String orderBy, boolean asc);

    List<TagVO> listTags();

    boolean createTag(CreateTagDTO dto);

    boolean updateTag(UpdateTagDTO dto);

    boolean deleteTag(Long id);

    Tag getTagById(Long id);
}