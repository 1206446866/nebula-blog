package com.nebula.api.facade;

import cn.hutool.core.bean.BeanUtil;
import com.nebula.api.vo.tag.home.TagHomeVO;
import com.nebula.article.service.ArticleTagService;
import com.nebula.tag.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TagHomeFacade {

    private final TagService tagService;
    private final ArticleTagService articleTagService;

    public List<TagHomeVO> getTagHome() {
        Map<Long, Long> articleCountByTag = articleTagService.getArticleCountByTag();
        return tagService.list().stream().map(item -> {
            TagHomeVO tagHomeVO = BeanUtil.copyProperties(item, TagHomeVO.class);
            tagHomeVO.setArticleCount(articleCountByTag.getOrDefault(item.getId(), 0L));
            return tagHomeVO;
        }).toList();
    }
}
