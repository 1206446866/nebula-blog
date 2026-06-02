package com.nebula.comment.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.nebula.auth.util.SecurityUtils;
import com.nebula.comment.dto.ReleaseCommentDto;
import com.nebula.comment.entity.Comment;
import com.nebula.comment.mapper.CommentMapper;
import com.nebula.comment.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.nebula.comment.entity.table.CommentTableDef.COMMENT;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    private final SecurityUtils securityUtils;

    @Override
    public Page<Comment> pageCommentsByArticleId(int page, int size, Long articleId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(COMMENT.ARTICLE_ID.eq(articleId))
                .orderBy(COMMENT.CREATE_TIME.desc());
        return page(Page.of(page,size),queryWrapper);
    }

    @Override
    public Boolean releaseComment(ReleaseCommentDto comment) {
        return save(Comment.creat().setUserId(securityUtils.getUserId()).setArticleId(comment.getArticleId()).setContent(comment.getContent()));
    }

    @Override
    public Boolean deleteCommentById(Long id) {
        return removeById(id);
    }

    @Override
    public Comment getCommentById(Long id) {
        return null;
    }

    @Override
    public boolean deleteCommentsBatch(List<Long> ids) {
        return false;
    }

    @Override
    public boolean updateStatusBatch(List<Long> ids, Integer status) {
        return false;
    }
}
