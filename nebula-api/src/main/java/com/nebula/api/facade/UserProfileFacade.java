package com.nebula.api.facade;

import cn.hutool.core.bean.BeanUtil;
import com.mybatisflex.core.paginate.Page;
import com.nebula.api.dto.ProfileQueryDto;
import com.nebula.api.vo.profile.*;
import com.nebula.article.entity.Article;
import com.nebula.article.service.ArticleService;
import com.nebula.auth.entity.LoginLog;
import com.nebula.auth.service.LoginLogService;
import com.nebula.comment.entity.Comment;
import com.nebula.comment.service.CommentService;
import com.nebula.common.constant.ArticleStatus;
import com.nebula.common.exception.BusinessException;
import com.nebula.common.security.LoginUser;
import com.nebula.common.util.SecurityUtils;
import com.nebula.role.service.RoleService;
import com.nebula.user.entity.User;
import com.nebula.user.service.UserService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import static com.nebula.common.exception.code.AuthErrorCode.USER_NOT_FOUND;

@Service
public class UserProfileFacade {
    private final UserService userService;
    private final RoleService roleService;
    private final ArticleService articleService;
    private final CommentService commentService;
    private final LoginLogService loginLogService;
    private final Executor profileExecutor;

    public UserProfileFacade(UserService userService, RoleService roleService,
                             ArticleService articleService, CommentService commentService,
                             LoginLogService loginLogService,
                             @Qualifier("profileExecutor") Executor profileExecutor) {
        this.userService = userService;
        this.roleService = roleService;
        this.articleService = articleService;
        this.commentService = commentService;
        this.loginLogService = loginLogService;
        this.profileExecutor = profileExecutor;
    }

    public UserProfileVO getProfile(Long userId, ProfileQueryDto dto) {
        User user = userService.getUserById(userId);
        if (user == null) {
            throw new BusinessException(USER_NOT_FOUND);
        }
        // Capture ownership on the request thread; worker threads do not inherit authentication.
        LoginUser viewer = SecurityUtils.getLoginUser();
        boolean isSelf = viewer != null && Objects.equals(userId, viewer.getUserId());

        var rolesFuture = CompletableFuture.supplyAsync(() -> roleService.getDescriptions(userId), profileExecutor);
        var viewsFuture = CompletableFuture.supplyAsync(() -> articleService.getViewAllCount(userId), profileExecutor);
        var articleLikesFuture = CompletableFuture.supplyAsync(() -> articleService.getLikeAllCount(userId), profileExecutor);
        var commentLikesFuture = CompletableFuture.supplyAsync(() -> commentService.getLikeAllCount(userId), profileExecutor);
        var publicFuture = CompletableFuture.supplyAsync(() -> loadPage(dto.getPublicArticlePage(),
                page -> articleService.pageArticleProfile(userId, ArticleStatus.PUBLISHED.getCode(), page, dto.getPublicArticleSize())), profileExecutor);
        var commentsFuture = CompletableFuture.supplyAsync(() -> loadPage(dto.getCommentPage(),
                page -> commentService.getCommentPageByUserId(userId, page, dto.getCommentSize())), profileExecutor);
        CompletableFuture<Page<Article>> draftsFuture = isSelf
                ? CompletableFuture.supplyAsync(() -> loadPage(dto.getDraftArticlePage(),
                    page -> articleService.pageArticleProfile(userId, ArticleStatus.DRAFT.getCode(), page, dto.getDraftArticleSize())), profileExecutor)
                : CompletableFuture.completedFuture(Page.of(1, dto.getDraftArticleSize(), 0));
        CompletableFuture<List<LoginLog>> logsFuture = isSelf
                ? CompletableFuture.supplyAsync(() -> loginLogService.getLoginLog(userId), profileExecutor)
                : CompletableFuture.completedFuture(List.of());

        CompletableFuture.allOf(rolesFuture, viewsFuture, articleLikesFuture, commentLikesFuture,
                publicFuture, commentsFuture, draftsFuture, logsFuture).join();

        Page<Article> publicPage = publicFuture.join();
        Page<Comment> commentPage = commentsFuture.join();
        UserProfileInfoVO info = BeanUtil.copyProperties(user, UserProfileInfoVO.class);
        info.setRoles(rolesFuture.join());
        if (!isSelf) {
            info.setEmail(null).setPhone(null);
        }

        Map<Long, Long> commentCounts = commentService.getCountByArticleIds(
                publicPage.getRecords().stream().map(Article::getId).toList());
        Page<UserProfileArticleVO> publicArticles = publicPage.map(article -> toArticleVO(article)
                .setComments(commentCounts.getOrDefault(article.getId(), 0L)));
        Page<UserProfileArticleVO> draftArticles = isSelf ? draftsFuture.join().map(this::toArticleVO) : null;

        List<Long> commentedArticleIds = commentPage.getRecords().stream().map(Comment::getArticleId).distinct().toList();
        Map<Long, String> titles = commentedArticleIds.isEmpty() ? Map.of()
                : articleService.getArticleTitlesByCommentIds(commentedArticleIds).stream()
                    .collect(Collectors.toMap(Article::getId, Article::getTitle));
        Page<UserProfileCommentVO> comments = commentPage.map(comment -> {
            UserProfileCommentVO vo = BeanUtil.copyProperties(comment, UserProfileCommentVO.class);
            vo.setArticleTitle(titles.get(comment.getArticleId()));
            return vo;
        });

        UserProfileStatisticsVO statistics = UserProfileStatisticsVO.create()
                .setArticleCount(publicPage.getTotalRow()).setCommentCount(commentPage.getTotalRow())
                .setTotalViewCount(viewsFuture.join()).setLikeCount(articleLikesFuture.join() + commentLikesFuture.join());
        return UserProfileVO.create().setUserProfileInfoVO(info).setStatisticsVO(statistics)
                .setPublicArticles(publicArticles).setComments(comments).setDraftArticles(draftArticles)
                .setLoginLogs(logsFuture.join().stream()
                        .map(log -> BeanUtil.copyProperties(log, UserProfileLoginLogVO.class)).toList());
    }

    private UserProfileArticleVO toArticleVO(Article article) {
        UserProfileArticleVO vo = BeanUtil.copyProperties(article, UserProfileArticleVO.class);
        String content = article.getContent();
        vo.setContent(content != null && content.length() > 150 ? content.substring(0, 150) + "..." : content);
        return vo;
    }

    private <T> Page<T> loadPage(int requestedPage, IntFunction<Page<T>> loader) {
        Page<T> page = loader.apply(requestedPage);
        if (page.getTotalRow() == 0) {
            page.setPageNumber(1);
        } else if (page.getPageNumber() > page.getTotalPage()) {
            page = loader.apply((int) page.getTotalPage());
        }
        return page;
    }
}
