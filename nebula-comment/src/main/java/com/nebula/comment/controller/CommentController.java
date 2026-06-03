package com.nebula.comment.controller;

import com.mybatisflex.core.paginate.Page;
import com.nebula.comment.dto.ReleaseCommentDto;
import com.nebula.comment.service.CommentService;
import com.nebula.comment.vo.CommentVO;
import com.nebula.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
@Validated
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/article/{articleId}")
    public Result<Page<CommentVO>> pageComments(@RequestParam(required = false) Long articleId, @RequestParam(defaultValue = "1") Integer currentPage, @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(commentService.pageComments(currentPage, size, articleId));
    }

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
    @DeleteMapping("/delete/{id}")
    public Result<Boolean> deleteComment(@PathVariable Long id) {
        return Result.success(commentService.deleteCommentById(id));
    }

}
