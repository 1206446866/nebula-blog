package com.nebula.api.vo.profile;

import com.mybatisflex.core.paginate.Page;
import com.nebula.user.vo.PublicUserVO;

/** 游客主页只包含公开身份、文章、评论和统计。 */
public record PublicUserProfileVO(PublicUserVO userProfileInfoVO, UserProfileStatisticsVO statisticsVO,
                                  Page<UserProfileArticleVO> publicArticles, Page<UserProfileCommentVO> comments) {}
