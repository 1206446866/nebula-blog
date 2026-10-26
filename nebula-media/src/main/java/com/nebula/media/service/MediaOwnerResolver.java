package com.nebula.media.service;

/** 由用户业务提供资源所属账号的NID，上传目录不使用客户端传入的账号。 */
public interface MediaOwnerResolver {
    /**
     * 从用户记录获取上传目录使用的NID，用户不存在时拒绝操作。
     *
     * @param userId 资源所属用户ID
     * @return 用户账号NID
     */
    String requireNid(Long userId);
}
