package com.nebula.tag.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.row.Db;
import com.mybatisflex.core.util.StringUtil;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.nebula.tag.dto.CreateTagDTO;
import com.nebula.tag.dto.UpdateTagDTO;
import com.nebula.tag.entity.Tag;
import com.nebula.tag.mapper.TagMapper;
import com.nebula.tag.service.TagService;
import com.nebula.tag.vo.TagVO;
import com.nebula.common.exception.BusinessException;
import com.nebula.common.exception.code.ManagementErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

import static com.nebula.tag.entity.table.TagTableDef.TAG;

@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {

    @Override
    public Page<TagVO> pageTags(String name, int page, int size, String orderBy, boolean asc) {
        QueryWrapper queryWrapper = new QueryWrapper()
                .where(TAG.NAME.like(name, StringUtil::hasText));
        if (Objects.nonNull(orderBy))
            queryWrapper.orderBy(orderBy, asc);
        return pageAs(Page.of(page, size), queryWrapper, TagVO.class);
    }

    @Override
    public List<TagVO> listTags() {
        return listAs(QueryWrapper.create(), TagVO.class);
    }

    @Override
    public boolean createTag(CreateTagDTO dto) {
        return save(BeanUtil.copyProperties(dto, Tag.class));
    }

    @Override
    public boolean updateTag(UpdateTagDTO dto) {
        return updateById(BeanUtil.copyProperties(dto, Tag.class));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTag(Long id) {
        var tag = mapper.selectOneByQuery(QueryWrapper.create().where(TAG.ID.eq(id)).forUpdate());
        if (tag == null) return false;
        if (Db.selectOneBySql("SELECT atg.article_id FROM article_tag atg JOIN article a ON a.id=atg.article_id "
                + "WHERE atg.tag_id=? AND a.deleted=0 LIMIT 1", id) != null) {
            throw new BusinessException(ManagementErrorCode.TAG_IN_USE);
        }
        Db.deleteBySql("DELETE FROM article_tag WHERE tag_id=?", id);
        return removeById(id);
    }

}
