package com.nebula.common.security;

import java.util.Set;

/** 跨模块角色查询与校验，避免用户模块反向依赖角色模块。 */
public interface RoleCatalog {
    /**
     * 锁定并校验待分配角色均未删除，避免与角色删除并发冲突。
     *
     * @param roleIds 待校验的角色ID集合
     * @return 集合非空且所有角色都存在时为true
     */
    boolean allRolesExist(Set<Long> roleIds);
    /**
     * 数字按角色ID精确匹配，其他文本按角色编码或说明模糊匹配。
     *
     * @param filter 已去除首尾空白的筛选文本
     * @return 匹配的有效角色ID集合
     */
    Set<Long> findRoleIds(String filter);
}
