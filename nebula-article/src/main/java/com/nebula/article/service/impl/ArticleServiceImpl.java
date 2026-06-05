package com.nebula.article.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.core.util.StringUtil;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.nebula.article.dto.ChangeArticleStatusDto;
import com.nebula.article.entity.Article;
import com.nebula.article.mapper.ArticleMapper;
import com.nebula.article.service.ArticleService;
import com.nebula.article.vo.ArticleVO;
import com.nebula.common.constant.ArticleStatus;
import org.apache.ibatis.javassist.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

import static com.nebula.article.entity.table.ArticleTableDef.ARTICLE;

@Service
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {

    @Override
    public Page<Article> pageArticles(int page, int size, Long userId, String title, String author, String orderBy, boolean asc) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(ARTICLE.TITLE.like(title).when(StringUtil.hasText(title)))
                .and(ARTICLE.AUTHOR.eq(author,StringUtil::hasText))
                .and(ARTICLE.USER_ID.eq(userId, Objects::nonNull));
        if(Objects.nonNull(orderBy)){
                queryWrapper.orderBy(orderBy, asc);
        }
        return page(Page.of(page, size), queryWrapper);
    }

    @Override
    public boolean deleteArticlesBatch(List<Long> ids) {
        if (Objects.isNull(ids) || ids.isEmpty()) {return true;}
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(ARTICLE.ID.eq(ids));
        return remove(queryWrapper);
    }

    @Override
    public boolean updateStatusBatch(List<Long> ids, Integer status) {
        // TODO: 使用 QueryWrapper 批量更新状态
        return false;
    }

    @Override
    public boolean updateArticlePartial(Long id, Article article) {
        // TODO: 使用 QueryWrapper 更新指定字段
        return false;
    }

    @Override
    public ArticleVO getArticleById(Long id) throws NotFoundException {
        Article article = Article.create().where(ARTICLE.ID.eq(id)).one();
        if (Objects.isNull(article)) { throw new NotFoundException("文章不存在或未发布");}
        UpdateChain.of(Article.class)
                .setRaw(ARTICLE.VIEW_COUNT,ARTICLE.VIEW_COUNT.add(1))
                .where(ARTICLE.ID.eq(id))
                .update();
        return BeanUtil.copyProperties(article, ArticleVO.class);
    }

    @Override
    public boolean changeArticleStatus(ChangeArticleStatusDto dto) {
        return Article.create().setId(dto.getId()).setStatus(dto.getStatus()).updateById();
    }

    @Override
    public Page<Article> pagePublishedArticles(String title, Integer currentPage, Integer size) {
        QueryWrapper query = QueryWrapper.create()
                .where(ARTICLE.STATUS.eq(
                        ArticleStatus.PUBLISHED.getCode()
                ))
                .and(ARTICLE.TITLE.eq(title, StringUtil::hasText));
        return page(Page.of(currentPage, size), query);
    }
}
