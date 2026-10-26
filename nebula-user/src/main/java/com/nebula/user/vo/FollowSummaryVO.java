package com.nebula.user.vo;

/** 相对于当前登录用户的关系；计数不包含已注销用户。 */
public record FollowSummaryVO(long followingCount, long followerCount,
                              boolean following, boolean followedBy, boolean self, boolean canFollow) {}
