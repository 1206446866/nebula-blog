package com.nebula.api.vo.profile;


import com.mybatisflex.core.paginate.Page;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 整个个人主页需要的数据。
 */
@Data
@Accessors(chain = true)
public class UserProfileVO {


    private UserProfileInfoVO userProfileInfoVO;

    private UserProfileStatisticsVO  statisticsVO;

    /**
     * 公开文章列表
     */
    private Page<UserProfileArticleVO> publicArticles;

    /**
     * 评论列表
     */
    private Page<UserProfileCommentVO> comments;

    /**
     * 文章草稿列表
     */
    private Page<UserProfileArticleVO> draftArticles;


    public static UserProfileVO create() {
        return new UserProfileVO();
    }
}