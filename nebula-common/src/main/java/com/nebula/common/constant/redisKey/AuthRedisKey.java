package com.nebula.common.constant.redisKey;

public  final class AuthRedisKey {

    private AuthRedisKey() {}

    public static final String EMAIL_CAPTCHA =
            "auth:email:captcha:";

    public static final String EMAIL_CAPTCHA_CD =
            "auth:email:captcha:cd:";

    /**
     * 注册临时凭证
     */
    public static final String REGISTER_TOKEN =
            "auth:register:token:";
}
