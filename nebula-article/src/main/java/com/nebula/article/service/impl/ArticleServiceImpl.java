package com.nebula.article.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryMethods;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.core.util.StringUtil;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.nebula.article.dto.*;
import com.nebula.article.entity.Article;
import com.nebula.article.entity.ArticleCategory;
import com.nebula.article.entity.ArticleLike;
import com.nebula.article.entity.ArticleTag;
import com.nebula.article.mapper.ArticleCategoryMapper;
import com.nebula.article.mapper.ArticleLikeMapper;
import com.nebula.article.mapper.ArticleMapper;
import com.nebula.article.mapper.ArticleTagMapper;
import com.nebula.article.service.ArticleCategoryService;
import com.nebula.article.service.ArticleService;
import com.nebula.article.service.ArticleTagService;
import com.nebula.article.vo.ArticleVO;
import com.nebula.common.constant.ArticleStatus;
import com.nebula.common.constant.redisKey.ArticleRedisKey;
import com.nebula.common.exception.BusinessException;
import com.nebula.common.redis.RedisService;
import com.nebula.common.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.nebula.article.entity.table.ArticleCategoryTableDef.ARTICLE_CATEGORY;
import static com.nebula.article.entity.table.ArticleLikeTableDef.ARTICLE_LIKE;
import static com.nebula.article.entity.table.ArticleTableDef.ARTICLE;
import static com.nebula.article.entity.table.ArticleTagTableDef.ARTICLE_TAG;
import static com.nebula.common.constant.redisKey.ArticleRedisKey.ARTICLE_DETAIL;
import static com.nebula.common.constant.redisKey.ArticleRedisKey.ARTICLE_VIEW;
import static com.nebula.common.exception.code.ArticleErrorCode.*;

@Service
@RequiredArgsConstructor
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {

    private final ArticleCategoryMapper articleCategoryMapper;
    private final ArticleTagMapper articleTagMapper;
    private final ArticleCategoryService articleCategoryService;
    private final ArticleTagService articleTagService;
    private final ArticleLikeMapper articleLikeMapper;
    private final RedisService redisService;
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Shanghai");

    @Override
    public Page<ArticleVO> pageArticles(ArticlePageDTO dto, String orderBy, boolean asc) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(ARTICLE.TITLE.like(dto.getTitle()).when(StringUtil.hasText(dto.getTitle())))
                .and(ARTICLE.AUTHOR.eq(dto.getAuthor(), StringUtil::hasText))
                .and(ARTICLE.USER_ID.eq(dto.getUserId(), Objects::nonNull));
        if (Objects.nonNull(orderBy)) {
            queryWrapper.orderBy(orderBy, asc);
        }
        return pageAs(Page.of(dto.getCurrent(), dto.getSize()), queryWrapper, ArticleVO.class);
    }


    @Override
    public ArticleVO getArticleById(Long id) {
        Article exist = Article.create().select(ARTICLE.ID, ARTICLE.STATUS, ARTICLE.VIEW_COUNT)
                .where(ARTICLE.ID.eq(id)).one();
        if (Objects.isNull(exist)) {
            throw new BusinessException(ARTICLE_NOT_FOUND);
        }
        if (!exist.getStatus().equals(ArticleStatus.PUBLISHED.getCode()))
            throw new BusinessException(ARTICLE_NOT_PUBLISHED);

        String key = ARTICLE_DETAIL + id;
        ArticleVO cache = redisService.get(key);
        if (cache != null) {
            ArticleLike like = articleLikeMapper.selectOneByCondition(ARTICLE_LIKE.ARTICLE_ID.eq(id).and(ARTICLE_LIKE.USER_ID.eq(SecurityUtils.getUserId())));
            cache.setLiked(Objects.nonNull(like));
            boolean increased = increaseViewCount(id, SecurityUtils.getUserId());
            cache.setViewCount(exist.getViewCount() + (increased ? 1 : 0));
            return cache;
        }
        Article article = Article.create().where(ARTICLE.ID.eq(id)).one();
        increaseViewCount(id, SecurityUtils.getUserId());
        ArticleVO articleVO = BeanUtil.copyProperties(article, ArticleVO.class);
        ArticleCategory articleCategory = articleCategoryMapper.selectOneByCondition(ARTICLE_CATEGORY.ARTICLE_ID.eq(id));
        if (articleCategory != null) {
            articleVO.setCategoryId(articleCategory.getCategoryId());
        }
        List<ArticleTag> articleTags = articleTagMapper.selectListByQuery(
                QueryWrapper.create().select(ARTICLE_TAG.TAG_ID)
                        .where(ARTICLE_TAG.ARTICLE_ID.eq(id))
        );
        articleVO.setTagIds(articleTags.stream().map(ArticleTag::getTagId).toList());
        redisService.set(key, articleVO, 30, TimeUnit.MINUTES);
        ArticleLike like = articleLikeMapper.selectOneByCondition(ARTICLE_LIKE.ARTICLE_ID.eq(id).and(ARTICLE_LIKE.USER_ID.eq(SecurityUtils.getUserId())));
        articleVO.setLiked(Objects.nonNull(like));
        return articleVO;
    }

    private boolean increaseViewCount(Long articleId, Long userId) {
        if (userId == null) {
            return false;
        }
        String key = ARTICLE_VIEW + articleId + ":" + userId;

        if (redisService.hasKey(key)) {
            return false;
        }

        ZonedDateTime now = ZonedDateTime.now(ZONE_ID);

        ZonedDateTime tomorrow = now.toLocalDate().plusDays(1).atStartOfDay(ZONE_ID);

        long seconds = Duration.between(now, tomorrow).getSeconds();
        UpdateChain.of(Article.class)
                .setRaw(ARTICLE.VIEW_COUNT, ARTICLE.VIEW_COUNT.add(1))
                .where(ARTICLE.ID.eq(articleId))
                .update();
        redisService.set(key, 1, seconds, TimeUnit.SECONDS);
        return true;
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
        articleTagService.bindTags(article.getId(), dto.getTagIds());
        return res;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean updateArticle(UpdateArticleDto dto) {
        checkOwnerOrAdmin(dto.getId());
        redisService.delete(ArticleRedisKey.ARTICLE_DETAIL + dto.getId());
        boolean res = updateById(Article.create()
                .setId(dto.getId())
                .setTitle(dto.getTitle())
                .setContent(dto.getContent())
        );
        articleCategoryService.bindCategories(dto.getId(), dto.getCategoryId());
        articleTagService.bindTags(dto.getId(), dto.getTagIds());
        return res;
    }

    @Override
    public boolean changeArticleStatus(ChangeArticleStatusDto dto) {
        boolean update = Article.create().setId(dto.getId()).setStatus(dto.getStatus()).updateById();
        redisService.delete(ArticleRedisKey.ARTICLE_DETAIL + dto.getId());
        return update;
    }

    @Override
    public Page<ArticleVO> pagePublishedArticles(ArticlePageDTO dto) {
        QueryWrapper query = QueryWrapper.create()
                .where(ARTICLE.STATUS.eq(ArticleStatus.PUBLISHED.getCode()))
                .and(ARTICLE.TITLE.eq(dto.getTitle(), StringUtil::hasText));
        // 分类筛选
        if (Objects.nonNull(dto.getCategoryIds())) {
            query.innerJoin(ARTICLE_CATEGORY)
                    .on(ARTICLE.ID.eq(ARTICLE_CATEGORY.ARTICLE_ID))
                    .and(ARTICLE_CATEGORY.CATEGORY_ID.in(dto.getCategoryIds()));
        }
        // 标签筛选
        if (Objects.nonNull(dto.getTagIds())) {
            query.innerJoin(ARTICLE_TAG)
                    .on(ARTICLE.ID.eq(ARTICLE_TAG.ARTICLE_ID))
                    .and(ARTICLE_TAG.TAG_ID.in(dto.getTagIds()));
        }
        return pageAs(Page.of(dto.getCurrent(), dto.getSize()), query, ArticleVO.class);
    }

    @Override
    public Long getAllCount(Long userId) {
        return getMapper().selectCountByCondition(ARTICLE.USER_ID.eq(userId));
    }

    @Override
    public Long getViewAllCount(Long userId) {
        Long total = getMapper().selectObjectByQueryAs(QueryWrapper.create().select(QueryMethods.sum(ARTICLE.VIEW_COUNT)).where(ARTICLE.USER_ID.eq(userId)), Long.class);
        return total == null ? 0L : total;
    }

    @Override
    public Long getLikeAllCount(Long userId) {
        Long total = getMapper().selectObjectByQueryAs(QueryWrapper.create().select(QueryMethods.sum(ARTICLE.LIKE_COUNT)).where(ARTICLE.USER_ID.eq(userId)), Long.class);
        return total == null ? 0L : total;
    }

    @Override
    public Page<Article> pageArticleProfile(Long userId, Integer status, int currentPage, int pageSize) {
        QueryWrapper query = QueryWrapper.create().where(ARTICLE.USER_ID.eq(userId).and(ARTICLE.STATUS.eq(status)));
        return page(Page.of(currentPage, pageSize), query);
    }


    @Override
    public List<Article> getArticleTitlesByCommentIds(List<Long> list) {
        return getMapper().selectListByQuery(QueryWrapper.create().select(ARTICLE.ID, ARTICLE.TITLE).where(ARTICLE.ID.in(list, Objects.nonNull(list) && !list.isEmpty())));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean like(ArticleLikeDto articleLikeDto) {
        checkOwnerOrAdmin(articleLikeDto.getArticleId());
        try {
            ArticleLike entity = ArticleLike.create().setArticleId(articleLikeDto.getArticleId()).setUserId(articleLikeDto.getUserId());
            articleLikeMapper.insert(entity);
            UpdateChain.of(Article.class)
                    .setRaw(ARTICLE.LIKE_COUNT, ARTICLE.LIKE_COUNT.add(1))
                    .where(ARTICLE.ID.eq(articleLikeDto.getArticleId()))
                    .update();

            return true;

        } catch (DuplicateKeyException e) {
            articleLikeMapper.deleteByQuery(QueryWrapper.create()
                    .where(ARTICLE_LIKE.ARTICLE_ID.eq(articleLikeDto.getArticleId()))
                    .and(ARTICLE_LIKE.USER_ID.eq(articleLikeDto.getUserId()))
            );

            UpdateChain.of(Article.class)
                    .setRaw(ARTICLE.LIKE_COUNT, ARTICLE.LIKE_COUNT.subtract(1))
                    .where(ARTICLE.ID.eq(articleLikeDto.getArticleId()))
                    .update();

            return false;
        }
    }

    @Override
    public Boolean delete(Long id) {
        checkOwnerOrAdmin(id);
        Article.create().setId(id).removeById();
        return redisService.delete(ArticleRedisKey.ARTICLE_DETAIL + id);
    }

    private void checkOwnerOrAdmin(Long articleId) {
        Article article = mapper.selectOneById(articleId);
        if (article == null)
            throw new BusinessException(ARTICLE_NOT_FOUND);
        if (!ArticleStatus.PUBLISHED.getCode().equals(article.getStatus()))
            throw new BusinessException(ARTICLE_NOT_PUBLISHED);
        if (!SecurityUtils.isAdmin() && !SecurityUtils.isSelf(article.getUserId()))
            throw new BusinessException(ARTICLE_ACCESS_DENIED);
    }
}
