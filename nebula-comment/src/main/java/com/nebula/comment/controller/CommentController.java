package com.nebula.comment.controller;

import com.mybatisflex.core.paginate.Page;
import com.nebula.comment.dto.CommentLikeDto;
import com.nebula.comment.dto.ReleaseCommentDto;
import com.nebula.comment.service.CommentService;
import com.nebula.comment.vo.CommentVO;
import com.nebula.common.result.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

    @GetMapping("/threads")
    public Result<Page<CommentVO>> pageThreads(@RequestParam @Min(1) Long articleId,
                                               @RequestParam(defaultValue = "1") @Min(1) Integer currentPage,
                                               @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer size) {
        return Result.success(commentService.pageThreads(articleId, currentPage, size));
    }

    @GetMapping("/{rootId}/replies")
    public Result<Page<CommentVO>> pageReplies(@PathVariable @Min(1) Long rootId,
                                               @RequestParam(defaultValue = "1") @Min(1) Integer currentPage,
                                               @RequestParam(defaultValue = "5") @Min(1) @Max(100) Integer size) {
        return Result.success(commentService.pageReplies(rootId, currentPage, size));
    }

    @GetMapping
    public Result<Page<CommentVO>> pageComments(@RequestParam(required = false) @Min(1) Long articleId,
                                              @RequestParam(required = false) String content,
                                              @RequestParam(defaultValue = "1") @Min(1) Integer currentPage,
                                              @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer size) {
        return Result.success(commentService.pageComments(articleId,content,currentPage, size));
    }

    @PreAuthorize("hasAuthority('comment:create')")
    @PutMapping("/release")
    public Result<Boolean> releaseComment(@RequestBody @Valid ReleaseCommentDto dto) {
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
    public Result<Boolean> deleteComment(@PathVariable @Min(1) Long id) {
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
