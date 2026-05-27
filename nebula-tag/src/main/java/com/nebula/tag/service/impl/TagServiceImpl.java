package com.nebula.tag.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.nebula.tag.entity.Tag;
import com.nebula.tag.mapper.TagMapper;
import com.nebula.tag.service.TagService;
import org.springframework.stereotype.Service;

import java.util.List;

//TODO implements
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {

    @Override
    public Page<Tag> pageTags(int page, int size, String name, String orderBy, boolean asc) {
        return null;
    }

    @Override
    public boolean deleteTagsBatch(List<Long> ids) {
        return false;
    }

    @Override
    public boolean updateStatusBatch(List<Long> ids, Integer status) {
        return false;
    }

    @Override
    public boolean updateTagPartial(Long id, Tag tag) {
        return false;
    }

    @Override
    public Tag getTagById(Long id) {
        return null;
    }
}
