package com.nebula.tag.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.nebula.tag.entity.Tag;

import java.util.List;

public interface TagService extends IService<Tag> {

    Page<Tag> pageTags(int page, int size, String name, String orderBy, boolean asc);

    boolean deleteTagsBatch(List<Long> ids);

    boolean updateStatusBatch(List<Long> ids, Integer status);

    boolean updateTagPartial(Long id, Tag tag);

    Tag getTagById(Long id);
}