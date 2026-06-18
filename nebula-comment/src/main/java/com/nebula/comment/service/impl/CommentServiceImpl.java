package com.nebula.comment.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.util.StringUtil;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.nebula.comment.dto.ReleaseCommentDto;
import com.nebula.comment.entity.Comment;
import com.nebula.comment.mapper.CommentMapper;
import com.nebula.comment.service.CommentService;
import com.nebula.comment.vo.CommentVO;
import com.nebula.common.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

import static com.nebula.comment.entity.table.CommentTableDef.COMMENT;
import static com.nebula.user.entity.table.UserTableDef.USER;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    @Override
    public Page<CommentVO> pageComments(Long articleId, String content, int page, int size) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .select(COMMENT.ID,
                        USER.USERNAME,
                        COMMENT.ARTICLE_ID,
                        COMMENT.CONTENT,
                        COMMENT.CREATE_TIME
                )
                .from(COMMENT)
                .leftJoin(USER)
                .on(COMMENT.USER_ID.eq(USER.ID))
                .where(COMMENT.ARTICLE_ID.eq(articleId, Objects::nonNull))
                .and(COMMENT.CONTENT.like(content, StringUtil::hasText))
                .orderBy(COMMENT.CREATE_TIME.desc());
        return getMapper().paginateAs(page, size, queryWrapper, CommentVO.class);
    }

    @Override
    public Boolean releaseComment(ReleaseCommentDto comment) {
        return save(Comment.creat().setUserId(SecurityUtils.getUserId()).setArticleId(comment.getArticleId()).setContent(comment.getContent()));
    }

    @Override
    public Boolean deleteCommentById(Long id) {
        return removeById(id);
    }


    @Override
    public boolean deleteCommentsBatch(List<Long> ids) {
        return false;
    }

    @Override
    public Long getAllCount(Long userId) {
        return getMapper().selectCountByCondition(COMMENT.USER_ID.eq(userId));
    }

    @Override
    public Page<Comment> getCommentPageByUserId(Long userId, int currentPage, int size) {
        return page(Page.of(currentPage,size),QueryWrapper.create().where(COMMENT.USER_ID.eq(userId)));
    }
}
