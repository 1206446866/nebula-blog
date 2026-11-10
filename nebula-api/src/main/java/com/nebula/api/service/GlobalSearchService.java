package com.nebula.api.service;

import com.nebula.api.vo.search.SearchResultVO;

/** 使用独立检索索引搜索站内公开内容，由本地MySQL核验当前可见性和展示资料。 */
public interface GlobalSearchService {
    /**
     * 按当前登录用户的读取权限搜索已发布文章、公开评论、用户、分类及标签。
     * 查询使用限时只读事务；HTTP入口另受账号、来源IP和并发预算限制。
     * @param keyword 搜索词，最多50字符、为有效Unicode且不含NUL；空白返回空结果，纯不可检索字符被拒绝
     * @param type all、article、comment、user、category或tag
     * @param current 页码，从1开始；已遍历完的结果越界返回最后一页，大结果集限制检索窗口
     * @param size 每页条数，1到50
     * @return 公开结果、类型数量及计数是否完整；索引未就绪或超时返回可重试错误，未完整核验时数量为估计值
     */
    SearchResultVO search(String keyword, String type, int current, int size);
}
