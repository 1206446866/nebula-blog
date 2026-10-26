package com.nebula.api.service;

import com.nebula.api.vo.search.SearchResultVO;

/** 使用数据库查询站内公开内容，不依赖外部搜索引擎。 */
public interface GlobalSearchService {
    /**
     * 按当前登录用户的读取权限搜索已发布文章、公开评论、用户、分类及标签。
     * 查询使用限时只读事务；HTTP入口另受账号、来源IP和并发预算限制。
     * @param keyword 搜索词，最多50字符；空白时返回空结果，不扫描全站
     * @param type all、article、comment、user、category或tag
     * @param current 页码，从1开始；越界返回最后一页
     * @param size 每页条数，1到50
     * @return 选定类型的分页与各可访问类型的数量；任何用户均看不到草稿命中
     */
    SearchResultVO search(String keyword, String type, int current, int size);
}
