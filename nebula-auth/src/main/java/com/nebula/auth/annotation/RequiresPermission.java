package com.nebula.auth.annotation;

import java.lang.annotation.*;

/**
 * 注解：接口权限校验
 * 使用示例：@RequiresPermission("role:assign")
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresPermission {

    /**
     * 权限标识
     */
    String value();
}