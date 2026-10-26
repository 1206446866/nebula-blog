package com.nebula.user.service;

import com.mybatisflex.core.paginate.Page;
import com.nebula.user.vo.CommunityUserVO;
import com.nebula.user.vo.FollowSummaryVO;

/** 登录用户之间的关注关系及社区公开资料查询。 */
public interface UserFollowService {
    /**
     * 设置当前登录用户对目标的关注状态，重复请求不累加关系。
     * @param targetId 目标用户ID，不能为本人；关注时目标必须正常且未注销
     * @param following true关注，false取消关注；禁用目标仍可取消关注
     * @return 目标用户最新的计数及相对当前登录用户的关系
     */
    FollowSummaryVO setFollowing(Long targetId, boolean following);

    /**
     * 查询用户关注数、粉丝数及双向关系，排除已注销账号。
     * @param userId 未注销的用户ID
     * @return 当前关系和计数；本人不能关注自己
     */
    FollowSummaryVO summary(Long userId);

    /**
     * 分页查询关注或粉丝，按最近关注时间排序，只返回公开资料。
     * @param userId 未注销的主页用户ID
     * @param followers true查询粉丝，false查询关注
     * @param current 页码，从1开始，越界返回最后一页
     * @param size 每页条数，1到100
     * @return 排除已注销账号的用户分页及其与当前登录用户的双向关系
     */
    Page<CommunityUserVO> connections(Long userId, boolean followers, int current, int size);

    /**
     * 发现正常状态的社区用户，排除本人，支持昵称和NID搜索。
     * @param keyword 关键词，可为空，最多50字符
     * @param current 页码，从1开始，越界返回最后一页
     * @param size 每页条数，1到100
     * @return 用户公开资料及相对当前登录用户的关系
     */
    Page<CommunityUserVO> discover(String keyword, int current, int size);
}
