package com.nebula.api.search;

import java.util.Map;

/** 全站检索索引的可靠同步与管理入口，MySQL始终保存业务事实。 */
public interface SearchIndexService {
    /** 在单次有界轮询内投递持久任务；ES失败不会撤销已提交的业务变更。 */
    void synchronize();
    /** 返回同步状态和待处理数量，不返回凭据、原始正文或数据库连接信息。 */
    Map<String, Object> status();
    /** 申请重新构建独立索引，完成后切换别名，保留上一份用于恢复。 */
    void requestRebuild();
    /** 校验本机索引完整建立后返回检索别名，未就绪时提供可重试的503响应。 */
    String readyAlias();
    /** 判断当前业务快照是否仍有待同步任务；返回false时搜索数量必须标为估计值。 */
    boolean synchronizedWithSource();
}
