package com.nebula.api.facade;

import cn.hutool.core.bean.BeanUtil;
import com.mybatisflex.core.paginate.Page;
import com.nebula.api.dto.ProfileQueryDto;
import com.nebula.api.vo.profile.*;
import com.nebula.article.entity.Article;
import com.nebula.article.service.ArticleService;
import com.nebula.comment.entity.Comment;
import com.nebula.comment.service.CommentService;
import com.nebula.common.constant.ArticleStatus;
import com.nebula.role.service.RoleService;
import com.nebula.user.entity.User;
import com.nebula.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserProfileFacade {

    private final UserService userService;
    private final RoleService roleService;
    private final ArticleService articleService;
    private final CommentService commentService;


    /**
     * 获取用户主页信息
     * <p>
     * 包含用户基础信息、文章统计、评论统计、
     * 总浏览量以及最近发布的文章列表。
     *
     * @return 用户主页信息
     */
    public UserProfileVO getProfile(Long userId,ProfileQueryDto dto) {
        UserProfileVO userProfileVO = new UserProfileVO();
//        用户信息和角色描述tag
        User user = userService.getUserById(userId);
        List<String> roleDescriptions = roleService.getDescriptions(userId);
        UserProfileInfoVO userProfileInfoVO = BeanUtil.copyProperties(user, UserProfileInfoVO.class);
        userProfileInfoVO.setRoles(roleDescriptions);

        Long articleCount = articleService.getAllCount(userId);
        Long commentCount = commentService.getAllCount(userId);
        Long articleViewAllCount = articleService.getViewAllCount(userId);
        UserProfileStatisticsVO userProfileStatisticsVO = UserProfileStatisticsVO.create().setArticleCount(articleCount).setCommentCount(commentCount).setTotalViewCount(articleViewAllCount);

        Page<Article> articlePage = articleService.pageArticleProfile(userId, ArticleStatus.PUBLISHED.getCode(), dto.getPublicArticlePage(), dto.getPublicArticleSize());
        Page<UserProfileArticleVO> publicArticles = articlePage.map(article -> BeanUtil.copyProperties(article, UserProfileArticleVO.class));

        articlePage = articleService.pageArticleProfile(userId, ArticleStatus.DRAFT.getCode(), dto.getDraftArticlePage(), dto.getDraftArticleSize());
        Page<UserProfileArticleVO> draftArticles = articlePage.map(article -> BeanUtil.copyProperties(article, UserProfileArticleVO.class));

        Page<Comment> commentPage = commentService.getCommentPageByUserId(userId, dto.getCommentPage(), dto.getCommentSize());
        Page<UserProfileCommentVO> comments = commentPage.map(comment -> BeanUtil.copyProperties(comment, UserProfileCommentVO.class));
        return userProfileVO.setUserProfileInfoVO(userProfileInfoVO).setStatisticsVO(userProfileStatisticsVO).setPublicArticles(publicArticles)
                .setDraftArticles(draftArticles).setComments(comments);
    }

}
