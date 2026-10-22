package com.nebula.common.security;

import java.util.Set;

/** Cross-module role validation without coupling nebula-user back to nebula-role. */
public interface RoleCatalog {
    boolean allRolesExist(Set<Long> roleIds);
    Set<Long> findRoleIds(String filter);
}
