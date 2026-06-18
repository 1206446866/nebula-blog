package com.nebula.article.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryMethods;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.core.util.StringUtil;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.nebula.article.dto.ArticlePageDTO;
import com.nebula.article.dto.ChangeArticleStatusDto;
import com.nebula.article.dto.CreateArticleDto;
import com.nebula.article.dto.UpdateArticleDto;
import com.nebula.article.entity.Article;
import com.nebula.article.entity.ArticleCategory;
import com.nebula.article.entity.ArticleTag;
import com.nebula.article.mapper.ArticleCategoryMapper;
import com.nebula.article.mapper.ArticleMapper;
import com.nebula.article.mapper.ArticleTagMapper;
import com.nebula.article.service.ArticleService;
import com.nebula.article.vo.ArticleVO;
import com.nebula.common.constant.ArticleStatus;
import com.nebula.common.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.javassist.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

import static com.nebula.article.entity.table.ArticleCategoryTableDef.ARTICLE_CATEGORY;
import static com.nebula.article.entity.table.ArticleTableDef.ARTICLE;
import static com.nebula.article.entity.table.ArticleTagTableDef.ARTICLE_TAG;

@Service
@RequiredArgsConstructor
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {

    private final ArticleCategoryMapper articleCategoryMapper;
    private final ArticleTagMapper articleTagMapper;


    @Override
    public Page<ArticleVO> pageArticles(ArticlePageDTO dto, String orderBy, boolean asc) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(ARTICLE.TITLE.like(dto.getTitle()).when(StringUtil.hasText(dto.getTitle())))
                .and(ARTICLE.AUTHOR.eq(dto.getAuthor(), StringUtil::hasText))
                .and(ARTICLE.USER_ID.eq(dto.getUserId(), Objects::nonNull));
        if (Objects.nonNull(orderBy)) {
            queryWrapper.orderBy(orderBy, asc);
        }
        return pageAs(Page.of(dto.getCurrentPage(), dto.getSize()), queryWrapper, ArticleVO.class);
    }


    @Override
    public ArticleVO getArticleById(Long id) throws NotFoundException {
        Article article = Article.create().where(ARTICLE.ID.eq(id)).one();
        if (Objects.isNull(article)) {
            throw new NotFoundException("文章不存在或未发布");
        }
        UpdateChain.of(Article.class)
                .setRaw(ARTICLE.VIEW_COUNT, ARTICLE.VIEW_COUNT.add(1))
                .where(ARTICLE.ID.eq(id))
                .update();
        ArticleVO articleVO = BeanUtil.copyProperties(article, ArticleVO.class);
        ArticleCategory articleCategory = articleCategoryMapper.selectOneByCondition(ARTICLE_CATEGORY.ARTICLE_ID.eq(id));
        if (articleCategory != null) {
            articleVO.setCategoryId(articleCategory.getCategoryId());
        }
        List<ArticleTag> articleTags =
                articleTagMapper.selectListByQuery(
                        QueryWrapper.create().select(ARTICLE_TAG.TAG_ID)
                                .where(
                                        ARTICLE_TAG.ARTICLE_ID.eq(id)
                                )
                );
        articleVO.setTagIds(
                articleTags.stream()
                        .map(ArticleTag::getTagId)
                        .toList()
        );
        return articleVO;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean createArticle(CreateArticleDto dto) {
//        草稿态
        Article article = Article.create().setUserId(SecurityUtils.getUserId())
                .setAuthor(Objects.requireNonNull(SecurityUtils.getLoginUser()).getUsername())
                .setTitle(dto.getTitle()).setContent(dto.getContent())
                .setStatus(ArticleStatus.DRAFT.getCode());
        boolean res = article.save();
        articleCategoryMapper.insertSelective(ArticleCategory.create().setArticleId(article.getId()).setCategoryId(dto.getCategoryId()));
        if (CollUtil.isNotEmpty(dto.getTagIds())) {
            List<ArticleTag> articleTags = dto.getTagIds()
                    .stream()
                    .map(tagId -> ArticleTag.create()
                            .setArticleId(article.getId())
                            .setTagId(tagId))
                    .toList();
            articleTagMapper.insertBatchSelective(articleTags);
        }
        return res;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean updateArticle(UpdateArticleDto dto) {
        boolean res = updateById(Article.create()
                .setId(dto.getId())
                .setTitle(dto.getTitle())
                .setContent(dto.getContent())
        );

        // 分类重建
        QueryWrapper categoryQuery = QueryWrapper.create()
                .where(ARTICLE_CATEGORY.ARTICLE_ID.eq(dto.getId()));
        //删除原有关系
        articleCategoryMapper.deleteByQuery(categoryQuery);
        //建立新关系
        articleCategoryMapper.insert(
                ArticleCategory.create()
                        .setArticleId(dto.getId())
                        .setCategoryId(dto.getCategoryId())
        );

        // 标签重建
        QueryWrapper tagQuery = QueryWrapper.create()
                .where(ARTICLE_TAG.ARTICLE_ID.eq(dto.getId()));

        articleTagMapper.deleteByQuery(tagQuery);

        if (CollUtil.isNotEmpty(dto.getTagIds())) {
            List<ArticleTag> articleTags = dto.getTagIds()
                    .stream()
                    .map(tagId -> ArticleTag.create()
                            .setArticleId(dto.getId())
                            .setTagId(tagId))
                    .toList();

            articleTagMapper.insertBatch(articleTags);
        }
        return res;
    }

    @Override
    public boolean changeArticleStatus(ChangeArticleStatusDto dto) {
        return Article.create().setId(dto.getId()).setStatus(dto.getStatus()).updateById();
    }

    @Override
    public Page<ArticleVO> pagePublishedArticles(ArticlePageDTO dto) {
        QueryWrapper query = QueryWrapper.create()
                .where(ARTICLE.STATUS.eq(
                        ArticleStatus.PUBLISHED.getCode()
                ))
                .and(ARTICLE.TITLE.eq(dto.getTitle(), StringUtil::hasText));
        // 分类筛选
        if (Objects.nonNull(dto.getCategoryId())) {
            query.innerJoin(ARTICLE_CATEGORY)
                    .on(ARTICLE.ID.eq(ARTICLE_CATEGORY.ARTICLE_ID))
                    .and(ARTICLE_CATEGORY.CATEGORY_ID.eq(dto.getCategoryId()));
        }
        // 标签筛选
        if (Objects.nonNull(dto.getTagId())) {
            query.innerJoin(ARTICLE_TAG)
                    .on(ARTICLE.ID.eq(ARTICLE_TAG.ARTICLE_ID))
                    .and(ARTICLE_TAG.TAG_ID.eq(dto.getTagId()));
        }
        return pageAs(Page.of(dto.getCurrentPage(), dto.getSize()), query, ArticleVO.class);
    }

    @Override
    public Long getAllCount(Long userId) {
        return getMapper().selectCountByCondition(ARTICLE.USER_ID.eq(userId));
    }

    @Override
    public Long getViewAllCount(Long userId) {
        Long total =getMapper().selectObjectByQueryAs(QueryWrapper.create().select(QueryMethods.sum(ARTICLE.VIEW_COUNT)).where(ARTICLE.USER_ID.eq(userId)),Long.class);
        return total == null ? 0L : total;
    }


    @Override
    public Page<Article> pageArticleProfile(Long userId,Integer status, int currentPage, int pageSize) {
        QueryWrapper query = QueryWrapper.create().where(ARTICLE.USER_ID.eq(userId).and(ARTICLE.STATUS.eq(status)));
        return page(Page.of(currentPage,pageSize),query);
    }

}
