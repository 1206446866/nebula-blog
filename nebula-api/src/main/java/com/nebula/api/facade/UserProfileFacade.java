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
import java.util.stream.Collectors;

@Service
public class UserProfileFacade {

    private final UserService userService;
    private final RoleService roleService;
    private final ArticleService articleService;
    private final CommentService commentService;
    private final LoginLogService loginLogService;
    private final Executor profileExecutor;

    public UserProfileFacade(
            UserService userService,
            RoleService roleService,
            ArticleService articleService,
            CommentService commentService, LoginLogService loginLogService,
            @Qualifier("profileExecutor") Executor profileExecutor) {

        this.userService = userService;
        this.roleService = roleService;
        this.articleService = articleService;
        this.commentService = commentService;
        this.loginLogService = loginLogService;
        this.profileExecutor = profileExecutor;
    }

    /**
     * 获取用户主页信息
     * <p>
     * 包含用户基础信息、文章统计、评论统计、
     * 总浏览量以及最近发布的文章列表。
     *
     * @return 用户主页信息
     */
    public UserProfileVO getProfile(Long userId, ProfileQueryDto dto) {
        UserProfileVO userProfileVO = new UserProfileVO();
        CompletableFuture<User> userFuture =
                CompletableFuture.supplyAsync(
                        () -> userService.getUserById(userId),
                        profileExecutor);
        CompletableFuture<List<String>> roleDescriptionsFuture =
                CompletableFuture.supplyAsync(
                        () -> roleService.getDescriptions(userId),
                        profileExecutor);

        CompletableFuture<Long> articleCountFuture =
                CompletableFuture.supplyAsync(
                        () -> articleService.getAllCount(userId),
                        profileExecutor);

        CompletableFuture<Long> commentCountFuture =
                CompletableFuture.supplyAsync(
                        () -> commentService.getAllCount(userId),
                        profileExecutor);

        CompletableFuture<Long> viewCountFuture =
                CompletableFuture.supplyAsync(
                        () -> articleService.getViewAllCount(userId),
                        profileExecutor);

        CompletableFuture<Long> articleLikeCountFuture =
                CompletableFuture.supplyAsync(
                        () -> articleService.getLikeAllCount(userId),
                        profileExecutor);

        CompletableFuture<Long> commentLikeCountFuture =
                CompletableFuture.supplyAsync(
                        () -> commentService.getLikeAllCount(userId),
                        profileExecutor);

        CompletableFuture<Page<Article>> publicPageFuture =
                CompletableFuture.supplyAsync(
                        () -> articleService.pageArticleProfile(
                                userId,
                                ArticleStatus.PUBLISHED.getCode(),
                                dto.getPublicArticlePage(),
                                dto.getPublicArticleSize()),
                        profileExecutor);

        CompletableFuture<Page<Article>> draftPageFuture =
                CompletableFuture.supplyAsync(
                        () -> articleService.pageArticleProfile(
                                userId,
                                ArticleStatus.DRAFT.getCode(),
                                dto.getDraftArticlePage(),
                                dto.getDraftArticleSize()),
                        profileExecutor);

        CompletableFuture<Page<Comment>> commentPageFuture =
                CompletableFuture.supplyAsync(
                        () -> commentService.getCommentPageByUserId(
                                userId,
                                dto.getCommentPage(),
                                dto.getCommentSize()),
                        profileExecutor);

        CompletableFuture<List<LoginLog>> loginLogFuture =
                CompletableFuture.supplyAsync(
                        () -> loginLogService.getLoginLog(userId),
                        profileExecutor);
        CompletableFuture.allOf(
                userFuture,
                roleDescriptionsFuture,
                articleCountFuture,
                commentCountFuture,
                viewCountFuture,
                articleLikeCountFuture,
                commentLikeCountFuture,
                publicPageFuture,
                draftPageFuture,
                commentPageFuture,
                loginLogFuture
        ).join();

        User user = userFuture.join();

        List<String> roleDescriptions = roleDescriptionsFuture.join();

        Long articleCount = articleCountFuture.join();

        Long commentCount = commentCountFuture.join();

        Long articleViewAllCount = viewCountFuture.join();

        Long articleLikeAllCount = articleLikeCountFuture.join();

        Long commentLikeAllCount = commentLikeCountFuture.join();

        Page<Article> publicArticlePage = publicPageFuture.join();

        Page<Article> draftArticlePage = draftPageFuture.join();

        Page<Comment> commentPage = commentPageFuture.join();

        List<LoginLog> loginLogsList = loginLogFuture.join();
        if (Objects.equals(user.getId(), SecurityUtils.getUserId())) {
            List<UserProfileLoginLogVO> loginLogVOS = loginLogsList.stream().map(item -> BeanUtil.copyProperties(item, UserProfileLoginLogVO.class)).toList();
            userProfileVO.setLoginLogs(loginLogVOS);
        }

        UserProfileInfoVO userProfileInfoVO = BeanUtil.copyProperties(user, UserProfileInfoVO.class);
        userProfileInfoVO.setRoles(roleDescriptions);

        UserProfileStatisticsVO userProfileStatisticsVO = UserProfileStatisticsVO.create().setArticleCount(articleCount).setCommentCount(commentCount)
                .setTotalViewCount(articleViewAllCount).setLikeCount(articleLikeAllCount + commentLikeAllCount);

        Map<Long, Long> countByArticleIds = commentService.getCountByArticleIds(publicArticlePage.getRecords().stream().map(Article::getId).toList());
        Page<UserProfileArticleVO> publicArticles = publicArticlePage.map(article -> {
            UserProfileArticleVO vo = BeanUtil.copyProperties(article, UserProfileArticleVO.class);
            String content = article.getContent();
            vo.setContent(content != null && content.length() > 150 ? content.substring(0, 150) + "..." : content)
                    .setComments(countByArticleIds.getOrDefault(article.getId(), 0L));
            return vo;
        });

        Page<UserProfileArticleVO> draftArticles = draftArticlePage.map(article -> {
            UserProfileArticleVO vo = BeanUtil.copyProperties(article, UserProfileArticleVO.class);
            String content = article.getContent();
            vo.setContent(content != null && content.length() > 150 ? content.substring(0, 150) + "..." : content);
            return vo;
        });

        List<Article> articleTitles = articleService.getArticleTitlesByCommentIds(commentPage.getRecords().stream().map(Comment::getArticleId).distinct().toList());
        Map<Long, String> titleMap = articleTitles.stream().collect(Collectors.toMap(Article::getId, Article::getTitle));
//        转换VO并把对应标题写入
        Page<UserProfileCommentVO> comments = commentPage.map(comment -> {
            UserProfileCommentVO vo = BeanUtil.copyProperties(comment, UserProfileCommentVO.class);
            vo.setArticleTitle(titleMap.get(comment.getArticleId()));
            return vo;
        });
        return userProfileVO.setUserProfileInfoVO(userProfileInfoVO).setStatisticsVO(userProfileStatisticsVO).setPublicArticles(publicArticles).setDraftArticles(draftArticles).setComments(comments);
    }

}
