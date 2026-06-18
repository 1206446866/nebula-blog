package com.nebula.api.vo.profile;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 统计数据。
 */
@Data
@Accessors(chain=true)
public class UserProfileStatisticsVO {


    /**
     * 文章数量
     */
    private Long articleCount;

    /**
     * 评论数量
     */
    private Long commentCount;

    /**
     * 总浏览量
     */
    private Long totalViewCount;

    /**
     * 获赞总数
     */
    private Long likeCount = 99999999L;

    public static UserProfileStatisticsVO create(){
        return new UserProfileStatisticsVO();
    }
}
