package com.nebula.comment.controller;

import com.mybatisflex.core.paginate.Page;
import com.nebula.comment.dto.CommentLikeDto;
import com.nebula.comment.dto.ReleaseCommentDto;
import com.nebula.comment.service.CommentService;
import com.nebula.comment.vo.CommentVO;
import com.nebula.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
@Validated
public class CommentController {

    private final CommentService commentService;

    @GetMapping
    public Result<Page<CommentVO>> pageComments(@RequestParam(required = false) Long articleId, @RequestParam(required = false) String content,@RequestParam(defaultValue = "1") Integer currentPage, @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(commentService.pageComments(articleId,content,currentPage, size));
    }

    @PreAuthorize("hasAuthority('comment:create')")
    @PutMapping("/release")
    public Result<Boolean> releaseComment(@RequestBody ReleaseCommentDto dto) {
        return Result.success(commentService.releaseComment(dto));
    }

    /**
     * 发布评论
     *
     * @param id 评论发布请求参数
     * @return 是否发布成功
     */
    @PreAuthorize("hasAuthority('comment:delete')")
    @DeleteMapping("/delete/{id}")
    public Result<Boolean> deleteComment(@PathVariable Long id) {
        return Result.success(commentService.deleteCommentById(id));
    }

    /**
     * 评论点赞
     *
     * @param dto 评论点赞参数
     * @return 点赞状态，true-点赞成功，false-取消点赞
     */
    @PutMapping("/like")
    @PreAuthorize("hasAuthority('comment:update')")
    public Result<Boolean> like(@RequestBody @Valid CommentLikeDto dto) {
        return Result.success(commentService.like(dto));
    }
}
