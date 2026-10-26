package com.nebula.user.vo;

/** 社区列表只暴露公开资料，不返回邮箱、手机、角色或登录记录。 */
public record CommunityUserVO(Long id, String nid, String username, String avatar,
                              boolean following, boolean followedBy, boolean self, boolean canFollow) {}
