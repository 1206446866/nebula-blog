package com.nebula.common.security;

/** 账号完成逻辑注销后，在同一事务内通知各业务模块清理私人数据。 */
public record AccountCancelledEvent(long userId) {}
