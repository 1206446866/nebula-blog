package com.nebula.api.vo.search;

/** 搜索结果的最小公开资料；跳转目标由类型与ID决定，不返回私人字段。 */
public record SearchItemVO(String type, Long id, Long targetId, String title,
                           String excerpt, String avatar, String createTime) {}
