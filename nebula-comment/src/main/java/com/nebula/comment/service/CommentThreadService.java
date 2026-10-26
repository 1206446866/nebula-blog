package com.nebula.comment.service;

import cn.hutool.core.bean.BeanUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.row.Db;
import com.nebula.article.mapper.ArticleMapper;
import com.nebula.article.service.ArticleReadAccess;
import com.nebula.comment.dto.ReleaseCommentDto;
import com.nebula.comment.entity.Comment;
import com.nebula.comment.mapper.CommentLikeMapper;
import com.nebula.comment.mapper.CommentMapper;
import com.nebula.comment.vo.CommentVO;
import com.nebula.common.exception.BusinessException;
import com.nebula.common.util.PageLimits;
import com.nebula.common.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import java.util.*;

import static com.nebula.article.entity.table.ArticleTableDef.ARTICLE;
import static com.nebula.comment.entity.table.CommentTableDef.COMMENT;
import static com.nebula.comment.entity.table.CommentLikeTableDef.COMMENT_LIKE;
import static com.nebula.user.entity.table.UserTableDef.USER;
import static com.nebula.common.exception.code.ArticleErrorCode.ARTICLE_NOT_FOUND;
import static com.nebula.common.exception.code.CommentErrorCode.*;

/** Existing parent_id stores the actual reply target; descendants are paginated separately. */
@Service
@RequiredArgsConstructor
public class CommentThreadService {
    private final CommentMapper comments;
    private final CommentLikeMapper likes;
    private final ArticleMapper articles;
    private final ArticleReadAccess readAccess;

    // UNION DISTINCT also stops malformed historical cycles without duplicating replies.
    private static final String THREAD = "WITH RECURSIVE thread AS ("
            + "SELECT id,parent_id FROM comment WHERE id=? AND status=0 "
            + "UNION DISTINCT SELECT c.id,c.parent_id FROM comment c JOIN thread p ON c.parent_id=p.id "
            + "WHERE c.article_id=? AND c.status=0) ";

    @Transactional(readOnly = true)
    public Page<CommentVO> pageThreads(Long articleId, int page, int size) {
        PageLimits.requireValid(page, size);
        readAccess.requireReadableArticle(articleId);
        var query = QueryWrapper.create().select(COMMENT.ALL_COLUMNS, USER.USERNAME, USER.AVATAR)
                .from(COMMENT).leftJoin(USER).on(COMMENT.USER_ID.eq(USER.ID).and(USER.DELETED.eq(0)))
                .where(COMMENT.ARTICLE_ID.eq(articleId))
                .and(COMMENT.PARENT_ID.eq(0).or(COMMENT.PARENT_ID.isNull()))
                .orderBy(COMMENT.CREATE_TIME.desc(), COMMENT.ID.desc());
        Page<CommentVO> result = comments.paginateAs(page, size, query, CommentVO.class);
        if (result.getRecords().isEmpty()) return result;
        var roots = result.getRecords().stream().map(CommentVO::getId).toList();
        String placeholders = String.join(",", Collections.nCopies(roots.size(), "?"));
        var params = new ArrayList<Object>(roots); params.add(articleId);
        var counts = Db.selectListBySql("WITH RECURSIVE thread AS ("
                + "SELECT id AS root_id,id FROM comment WHERE id IN (" + placeholders + ") AND status=0 "
                + "UNION DISTINCT SELECT p.root_id,c.id FROM comment c JOIN thread p ON c.parent_id=p.id "
                + "WHERE c.article_id=? AND c.status=0) SELECT root_id,COUNT(*)-1 AS reply_count FROM thread GROUP BY root_id", params.toArray());
        Map<Long, Long> totals = new HashMap<>();
        counts.forEach(row -> totals.put(row.getLong("root_id"), row.getLong("reply_count")));
        var viewer = SecurityUtils.getLoginUser();
        Set<Long> liked = viewer == null ? Set.of() : new HashSet<>(likes.selectListByCondition(
                COMMENT_LIKE.COMMENT_ID.in(roots).and(COMMENT_LIKE.USER_ID.eq(viewer.getUserId())))
                .stream().map(com.nebula.comment.entity.CommentLike::getCommentId).toList());
        result.getRecords().forEach(comment -> comment.setRootId(comment.getId())
                .setReplyCount(totals.getOrDefault(comment.getId(), 0L)).setLiked(liked.contains(comment.getId())));
        return result;
    }

    @Transactional(readOnly = true)
    public Page<CommentVO> pageReplies(Long rootId, int page, int size) {
        PageLimits.requireValid(page, size);
        Comment root = requireComment(rootId);
        readAccess.requireReadableArticle(root.getArticleId());
        if (root.getParentId() != null && root.getParentId() != 0) throw new BusinessException(COMMENT_REPLY_INVALID);
        long count = Db.selectOneBySql(THREAD + "SELECT COUNT(*)-1 AS total FROM thread", rootId, root.getArticleId()).getLong("total");
        Page<CommentVO> result = Page.of(page, size, Math.max(0, count));
        var viewer = SecurityUtils.getLoginUser();
        Long viewerId = viewer == null ? -1L : viewer.getUserId();
        var rows = Db.selectListBySql(THREAD
                + "SELECT c.id,c.article_id AS articleId,c.user_id AS userId,c.parent_id AS parentId,"
                + "c.content,c.create_time AS createTime,c.like_count AS likeCount,u.username,u.avatar,"
                + "p.user_id AS replyToUserId,ru.username AS replyToUsername,"
                + "EXISTS(SELECT 1 FROM comment_like cl WHERE cl.comment_id=c.id AND cl.user_id=?) AS liked "
                + "FROM thread t JOIN comment c ON c.id=t.id "
                + "LEFT JOIN user u ON u.id=c.user_id AND u.deleted=0 "
                + "LEFT JOIN comment p ON p.id=c.parent_id AND p.status=0 "
                + "LEFT JOIN user ru ON ru.id=p.user_id AND ru.deleted=0 "
                + "WHERE c.id<>? ORDER BY c.create_time ASC,c.id ASC LIMIT ? OFFSET ?",
                rootId, root.getArticleId(), viewerId, rootId, size, (long) (page - 1) * size);
        result.setRecords(rows.stream().map(row -> BeanUtil.toBean(row, CommentVO.class)
                .setRootId(rootId).setReplyCount(0L)).toList());
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean release(ReleaseCommentDto dto) {
        requirePermission("comment:create");
        if (dto == null || dto.getArticleId() == null || dto.getArticleId() < 1 || dto.getContent() == null
                || dto.getContent().isBlank() || dto.getContent().length() > 2000
                || (dto.getParentId() != null && dto.getParentId() < 0)) throw new BusinessException(COMMENT_INPUT_INVALID);
        lockReadableArticle(dto.getArticleId());
        long parentId = dto.getParentId() == null ? 0L : dto.getParentId();
        if (parentId != 0) validateParentChain(parentId, dto.getArticleId());
        if (comments.insertSelective(Comment.create().setArticleId(dto.getArticleId()).setUserId(SecurityUtils.getUserId())
                .setParentId(parentId).setContent(dto.getContent().strip()).setStatus(0).setLikeCount(0L)) != 1) {
            throw new BusinessException(COMMENT_CREATE_FAILED);
        }
        return true;
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public Boolean delete(Long id) {
        requirePermission("comment:delete");
        Comment initial = requireComment(id);
        lockReadableArticle(initial.getArticleId());
        Comment target = comments.selectOneByQuery(QueryWrapper.create().where(COMMENT.ID.eq(id)).forUpdate());
        if (target == null) throw new BusinessException(COMMENT_NOT_FOUND);
        if (!SecurityUtils.isAdmin() && !Objects.equals(target.getUserId(), SecurityUtils.getUserId())) {
            throw new BusinessException(COMMENT_ACCESS_DENIED);
        }
        // Current locking reads include a reply committed while we were waiting for the article.
        // Keep this traversal on the same entity mapper/transaction as the eventual writes.
        Set<Long> ids = new LinkedHashSet<>();
        ids.add(id);
        List<Long> frontier = List.of(id);
        while (!frontier.isEmpty()) {
            var children = comments.selectListByQuery(QueryWrapper.create().where(COMMENT.ARTICLE_ID.eq(target.getArticleId()))
                    .and(COMMENT.PARENT_ID.in(frontier)).orderBy(COMMENT.ID.asc()).forUpdate());
            frontier = children.stream().map(Comment::getId).filter(ids::add).toList();
        }
        likes.deleteByCondition(COMMENT_LIKE.COMMENT_ID.in(ids));
        int affected = comments.deleteByQuery(QueryWrapper.create().where(COMMENT.ID.in(ids)));
        if (affected != ids.size()) throw new BusinessException(COMMENT_DELETE_FAILED);
        return true;
    }

    /** 锁定并检查回复父链，拒绝跨文章、循环、缺失目标和超过64层的回复。 */
    private void validateParentChain(Long parentId, Long articleId) {
        Set<Long> visited = new HashSet<>();
        Long next = parentId;
        for (int depth = 0; depth < 64; depth++) {
            if (!visited.add(next)) throw new BusinessException(COMMENT_REPLY_INVALID);
            Comment parent = comments.selectOneByQuery(QueryWrapper.create().where(COMMENT.ID.eq(next)).forUpdate());
            if (parent == null) throw new BusinessException(COMMENT_NOT_FOUND);
            if (!Objects.equals(parent.getArticleId(), articleId)) throw new BusinessException(COMMENT_REPLY_INVALID);
            if (parent.getParentId() == null || parent.getParentId() == 0) return;
            next = parent.getParentId();
        }
        throw new BusinessException(COMMENT_REPLY_TOO_DEEP);
    }

    /** 校验评论ID并读取未删除评论，无效目标统一返回评论不存在。 */
    private Comment requireComment(Long id) {
        if (id == null || id < 1) throw new BusinessException(COMMENT_NOT_FOUND);
        Comment comment = comments.selectOneById(id);
        if (comment == null) throw new BusinessException(COMMENT_NOT_FOUND);
        return comment;
    }

    /** 锁定当前可读文章，使评论写入与文章撤回、删除按顺序执行。 */
    private void lockReadableArticle(Long id) {
        if (articles.selectOneByQuery(QueryWrapper.create().where(ARTICLE.ID.eq(id))
                .and(readAccess.readableCondition()).forUpdate()) == null) throw new BusinessException(ARTICLE_NOT_FOUND);
    }

    /** 校验当前登录身份与对应评论权限，防止直接调用服务绕过接口鉴权。 */
    private void requirePermission(String permission) {
        var viewer = SecurityUtils.getLoginUser();
        if (viewer == null || viewer.getUserId() == null || !SecurityUtils.hasPermission(permission)) {
            throw new AccessDeniedException("需要对应的评论操作权限");
        }
    }
}
