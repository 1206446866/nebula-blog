package com.nebula.article.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryMethods;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.row.Db;
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
import com.nebula.article.service.ArticleReadAccess;
import com.nebula.article.service.ArticleCatalogReferences;
import com.nebula.article.service.ArticleTagService;
import com.nebula.article.vo.ArticleVO;
import com.nebula.common.constant.ArticleStatus;
import com.nebula.common.constant.redisKey.ArticleRedisKey;
import com.nebula.common.exception.BusinessException;
import com.nebula.common.redis.RedisService;
import com.nebula.common.util.SecurityUtils;
import com.nebula.common.util.PageLimits;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
    private final ArticleReadAccess articleReadAccess;
    private final ArticleCatalogReferences references;
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Shanghai");

    @Override
    public Page<ArticleVO> pageArticles(ArticlePageDTO dto, String orderBy, boolean asc) {
        PageLimits.requireValid(dto.getCurrent(), dto.getSize());
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(ARTICLE.TITLE.like(dto.getTitle()).when(StringUtil.hasText(dto.getTitle())))
                .and(ARTICLE.AUTHOR.eq(dto.getAuthor(), StringUtil::hasText))
                .and(ARTICLE.USER_ID.eq(dto.getUserId(), Objects::nonNull))
                .and(articleReadAccess.readableCondition());
        applyCatalogFilters(queryWrapper, dto);
        if (Objects.nonNull(orderBy)) {
            queryWrapper.orderBy(orderBy, asc);
        } else {
            queryWrapper.orderBy(ARTICLE.CREATE_TIME.desc(), ARTICLE.ID.desc());
        }
        return pageAs(Page.of(dto.getCurrent(), dto.getSize()), queryWrapper, ArticleVO.class);
    }


    @Override
    public ArticleVO getArticleById(Long id) {
        // Check current database visibility before reading a shared cache entry.
        Article article = articleReadAccess.requireReadableArticle(id);
        boolean published = ArticleStatus.PUBLISHED.getCode().equals(article.getStatus());
        var viewer = SecurityUtils.getLoginUser();
        Long viewerId = viewer == null ? null : viewer.getUserId();
        String key = ARTICLE_DETAIL + id;
        ArticleVO cached = published ? redisService.get(key) : null;
        ArticleVO articleVO = cached == null ? toDetailVO(article) : BeanUtil.copyProperties(cached, ArticleVO.class);
        populateCatalogReferences(articleVO, id);
        if (published) {
            if (cached == null) redisService.set(key, articleVO, 30, TimeUnit.MINUTES);
            boolean increased = increaseViewCount(id, viewerId);
            articleVO.setViewCount((article.getViewCount() == null ? 0L : article.getViewCount()) + (increased ? 1 : 0));
        }
        articleVO.setLiked(viewerId != null && articleLikeMapper.selectOneByCondition(
                ARTICLE_LIKE.ARTICLE_ID.eq(id).and(ARTICLE_LIKE.USER_ID.eq(viewerId))) != null);
        // Shared cached details must not override the current database counter.
        articleVO.setLikeCount(article.getLikeCount() == null ? 0 : article.getLikeCount());
        return articleVO;
    }

    private ArticleVO toDetailVO(Article article) {
        return BeanUtil.copyProperties(article, ArticleVO.class);
    }

    private void populateCatalogReferences(ArticleVO article, Long articleId) {
        var category = Db.selectOneBySql("SELECT ac.category_id FROM article_category ac JOIN category c ON c.id=ac.category_id "
                + "WHERE ac.article_id=? AND c.deleted=0 ORDER BY ac.category_id LIMIT 1", articleId);
        article.setCategoryId(category == null ? null : category.getLong("category_id"));
        article.setTagIds(Db.selectListBySql("SELECT DISTINCT atg.tag_id FROM article_tag atg JOIN tag t ON t.id=atg.tag_id "
                + "WHERE atg.article_id=? AND t.deleted=0 ORDER BY atg.tag_id", articleId)
                .stream().map(row -> row.getLong("tag_id")).toList());
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
        references.lockCategory(dto.getCategoryId());
        var tagIds = references.lockTags(dto.getTagIds());
//        草稿态
        Article article = Article.create().setUserId(SecurityUtils.getUserId())
                .setAuthor(Objects.requireNonNull(SecurityUtils.getLoginUser()).getUsername())
                .setTitle(dto.getTitle()).setContent(dto.getContent())
                .setStatus(ArticleStatus.DRAFT.getCode());
        if (mapper.insertSelective(article) != 1) throw new BusinessException(ARTICLE_CREATE_FAILED);
        articleCategoryService.bindCategories(article.getId(), dto.getCategoryId());
        articleTagService.bindTags(article.getId(), tagIds);
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean updateArticle(UpdateArticleDto dto) {
        references.lockCategory(dto.getCategoryId());
        var tagIds = references.lockTags(dto.getTagIds());
        checkOwnerOrAdmin(dto.getId(), "article:update");
        boolean res = updateById(Article.create()
                .setId(dto.getId())
                .setTitle(dto.getTitle())
                .setContent(dto.getContent())
        );
        if (!res) throw new BusinessException(ARTICLE_UPDATE_FAILED);
        articleCategoryService.bindCategories(dto.getId(), dto.getCategoryId());
        articleTagService.bindTags(dto.getId(), tagIds);
        evictDetailAfterCommit(dto.getId());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean changeArticleStatus(ChangeArticleStatusDto dto) {
        if (!SecurityUtils.isAdmin() || !SecurityUtils.hasPermission("article:update")) {
            throw new AccessDeniedException("需要文章管理权限");
        }
        if (dto == null || dto.getId() == null || dto.getId() < 1) throw new BusinessException(ARTICLE_NOT_FOUND);
        if (dto.getStatus() == null || dto.getStatus() < 0 || dto.getStatus() > 1) throw new BusinessException(ARTICLE_STATUS_INVALID);
        Article article = mapper.selectOneByQuery(QueryWrapper.create().where(ARTICLE.ID.eq(dto.getId())).forUpdate());
        if (article == null) throw new BusinessException(ARTICLE_NOT_FOUND);
        if (mapper.update(Article.create().setId(dto.getId()).setStatus(dto.getStatus())) != 1) {
            throw new BusinessException(ARTICLE_STATUS_UPDATE_FAILED);
        }
        evictDetailAfterCommit(dto.getId());
        return true;
    }

    @Override
    public Page<ArticleVO> pagePublishedArticles(ArticlePageDTO dto) {
        PageLimits.requireValid(dto.getCurrent(), dto.getSize());
        QueryWrapper query = QueryWrapper.create()
                .where(articleReadAccess.publishedCondition())
                .and(ARTICLE.TITLE.like(dto.getTitle(), StringUtil::hasText))
                .and(ARTICLE.AUTHOR.eq(dto.getAuthor(), StringUtil::hasText))
                .and(ARTICLE.USER_ID.eq(dto.getUserId(), Objects::nonNull));
        applyCatalogFilters(query, dto);
        query.orderBy(ARTICLE.CREATE_TIME.desc(), ARTICLE.ID.desc());
        return pageAs(Page.of(dto.getCurrent(), dto.getSize()), query, ArticleVO.class);
    }

    private void applyCatalogFilters(QueryWrapper query, ArticlePageDTO dto) {
        if (dto.getCategoryIds() != null && !dto.getCategoryIds().isEmpty()) {
            requireFilterIds(dto.getCategoryIds());
            query.and(ARTICLE.ID.in(QueryWrapper.create().select(ARTICLE_CATEGORY.ARTICLE_ID).from(ARTICLE_CATEGORY)
                    .where(ARTICLE_CATEGORY.CATEGORY_ID.in(dto.getCategoryIds()))
                    .and(ARTICLE_CATEGORY.CATEGORY_ID.in(QueryWrapper.create().select("id").from("category").where("deleted=0")))));
        }
        if (dto.getTagIds() != null && !dto.getTagIds().isEmpty()) {
            requireFilterIds(dto.getTagIds());
            query.and(ARTICLE.ID.in(QueryWrapper.create().select(ARTICLE_TAG.ARTICLE_ID).from(ARTICLE_TAG)
                    .where(ARTICLE_TAG.TAG_ID.in(dto.getTagIds()))
                    .and(ARTICLE_TAG.TAG_ID.in(QueryWrapper.create().select("id").from("tag").where("deleted=0")))));
        }
    }

    private void requireFilterIds(List<Long> ids) {
        if (ids.size() > 100 || ids.stream().anyMatch(id -> id == null || id < 1)) {
            throw new BusinessException(com.nebula.common.exception.code.ManagementErrorCode.TAG_INVALID);
        }
    }

    @Override
    public Long getAllCount(Long userId) {
        return getMapper().selectCountByCondition(ARTICLE.USER_ID.eq(userId));
    }

    @Override
    public Long getViewAllCount(Long userId) {
        Long total = getMapper().selectObjectByQueryAs(QueryWrapper.create().select(QueryMethods.sum(ARTICLE.VIEW_COUNT)).where(ARTICLE.USER_ID.eq(userId)).and(ARTICLE.STATUS.eq(ArticleStatus.PUBLISHED.getCode())), Long.class);
        return total == null ? 0L : total;
    }

    @Override
    public Long getLikeAllCount(Long userId) {
        Long total = getMapper().selectObjectByQueryAs(QueryWrapper.create().select(QueryMethods.sum(ARTICLE.LIKE_COUNT)).where(ARTICLE.USER_ID.eq(userId)).and(ARTICLE.STATUS.eq(ArticleStatus.PUBLISHED.getCode())), Long.class);
        return total == null ? 0L : total;
    }

    @Override
    public Page<Article> pageArticleProfile(Long userId, Integer status, int currentPage, int pageSize) {
        QueryWrapper query = QueryWrapper.create().where(ARTICLE.USER_ID.eq(userId).and(ARTICLE.STATUS.eq(status)))
                .orderBy(ARTICLE.CREATE_TIME.desc(), ARTICLE.ID.desc());
        return page(Page.of(currentPage, pageSize), query);
    }


    @Override
    public List<Article> getArticleTitlesByCommentIds(List<Long> list) {
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        return getMapper().selectListByQuery(QueryWrapper.create().select(ARTICLE.ID, ARTICLE.TITLE)
                .where(ARTICLE.ID.in(list)).and(articleReadAccess.publishedCondition()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean like(ArticleLikeDto articleLikeDto) {
        var actor = SecurityUtils.getLoginUser();
        if (actor == null || actor.getUserId() == null) throw new AccessDeniedException("请先登录");
        Long articleId = articleLikeDto.getArticleId();
        if (articleId == null || articleId < 1) throw new BusinessException(ARTICLE_NOT_FOUND);
        // Serialize interactions on this article, including the visibility check.
        // Row locking also prevents withdrawal/deletion halfway through a toggle.
        Article article = mapper.selectOneByQuery(QueryWrapper.create().where(ARTICLE.ID.eq(articleId))
                .and(articleReadAccess.publishedCondition()).forUpdate());
        if (article == null) throw new BusinessException(ARTICLE_NOT_FOUND);

        var condition = ARTICLE_LIKE.ARTICLE_ID.eq(articleId).and(ARTICLE_LIKE.USER_ID.eq(actor.getUserId()));
        ArticleLike existing = articleLikeMapper.selectOneByCondition(condition);
        boolean liked = existing == null;
        int affected = liked
                ? articleLikeMapper.insert(ArticleLike.create().setArticleId(articleId).setUserId(actor.getUserId()))
                : articleLikeMapper.deleteByCondition(condition);
        if (affected != 1) throw new BusinessException(ARTICLE_LIKE_FAILED);

        // Use the relationship rows as the source of truth, also repairing a stale
        // or null historical counter without decrementing it below zero.
        long total = articleLikeMapper.selectCountByCondition(ARTICLE_LIKE.ARTICLE_ID.eq(articleId));
        if (total > Integer.MAX_VALUE
                || mapper.update(Article.create().setId(articleId).setLikeCount((int) total)) != 1) {
            throw new BusinessException(ARTICLE_LIKE_FAILED);
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { redisService.delete(ARTICLE_DETAIL + articleId); }
        });
        return liked;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean delete(Long id) {
        checkOwnerOrAdmin(id, "article:delete");
        if (mapper.deleteById(id) != 1) throw new BusinessException(ARTICLE_DELETE_FAILED);
        evictDetailAfterCommit(id);
        return true;
    }

    private void evictDetailAfterCommit(Long articleId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { redisService.delete(ARTICLE_DETAIL + articleId); }
        });
    }

    private void checkOwnerOrAdmin(Long articleId, String permission) {
        var actor = SecurityUtils.getLoginUser();
        if (actor == null || actor.getUserId() == null || !SecurityUtils.hasPermission(permission)) {
            throw new AccessDeniedException("需要对应的文章操作权限");
        }
        if (articleId == null || articleId < 1) throw new BusinessException(ARTICLE_NOT_FOUND);
        Article article = mapper.selectOneByQuery(QueryWrapper.create().where(ARTICLE.ID.eq(articleId)).forUpdate());
        if (article == null)
            throw new BusinessException(ARTICLE_NOT_FOUND);
        // Writing a draft never publishes it; publication has its own guarded endpoint.
        if (!SecurityUtils.isAdmin() && !SecurityUtils.isSelf(article.getUserId()))
            throw new BusinessException(ARTICLE_ACCESS_DENIED);
    }
}
