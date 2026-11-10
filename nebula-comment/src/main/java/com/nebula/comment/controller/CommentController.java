package com.nebula.comment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "文章评论", description = "评论、回复分页及评论互动。")
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "分页查询文章评论主题",
            description = "检查目标文章读取权限，返回顶层评论及回复摘要。")
    @GetMapping("/threads")
    public Result<Page<CommentVO>> pageThreads(@Parameter(description = "文章编号") @RequestParam @Min(1) Long articleId,
                                               @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") @Min(1) Integer currentPage,
                                               @Parameter(description = "每页数量，以当前接口校验上限为准") @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer size) {
        return Result.success(commentService.pageThreads(articleId, currentPage, size));
    }

    @Operation(summary = "分页查询评论回复",
            description = "检查所属文章读取权限，返回指定根评论的回复。")
    @GetMapping("/{rootId}/replies")
    public Result<Page<CommentVO>> pageReplies(@Parameter(description = "根评论编号") @PathVariable @Min(1) Long rootId,
                                               @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") @Min(1) Integer currentPage,
                                               @Parameter(description = "每页数量，以当前接口校验上限为准") @RequestParam(defaultValue = "5") @Min(1) @Max(100) Integer size) {
        return Result.success(commentService.pageReplies(rootId, currentPage, size));
    }

    @Operation(summary = "分页查询评论",
            description = "按文章、内容等条件筛选，结果受当前账号和文章可见范围约束。")
    @GetMapping
    public Result<Page<CommentVO>> pageComments(@Parameter(description = "文章编号") @RequestParam(required = false) @Min(1) Long articleId,
                                              @Parameter(description = "评论内容筛选条件") @RequestParam(required = false) String content,
                                              @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") @Min(1) Integer currentPage,
                                              @Parameter(description = "每页数量，以当前接口校验上限为准") @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer size) {
        return Result.success(commentService.pageComments(articleId,content,currentPage, size));
    }

    @Operation(summary = "发布评论或回复",
            description = "需comment:create权限及目标文章读取权限，检查回复关系和正文内容。")
    @PreAuthorize("hasAuthority('comment:create')")
    @PutMapping("/release")
    public Result<Boolean> releaseComment(@RequestBody @Valid ReleaseCommentDto dto) {
        return Result.success(commentService.releaseComment(dto));
    }

    @Operation(summary = "删除评论",
            description = "需comment:delete权限，限评论本人或有权管理的管理员操作。")
    @PreAuthorize("hasAuthority('comment:delete')")
    @DeleteMapping("/delete/{id}")
    public Result<Boolean> deleteComment(@Parameter(description = "评论编号") @PathVariable @Min(1) Long id) {
        return Result.success(commentService.deleteCommentById(id));
    }

    @Operation(summary = "切换评论点赞状态",
            description = "需comment:update权限及所属文章读取权限；true为已点赞，false为未点赞。")
    @PutMapping("/like")
    @PreAuthorize("hasAuthority('comment:update')")
    public Result<Boolean> like(@RequestBody @Valid CommentLikeDto dto) {
        return Result.success(commentService.like(dto));
    }
}
