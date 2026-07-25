create table if not exists login_log
(
    id          bigint auto_increment
        primary key,
    user_id     bigint                             not null,
    login_type  varchar(20)                        null comment '登录方式',
    device      varchar(100)                       null comment '设备',
    ip          varchar(64)                        null comment 'IP地址',
    location    varchar(100)                       null comment '登录地点',
    status      int                                null comment '登录状态 0失败 1成功',
    create_time datetime default CURRENT_TIMESTAMP null,
    browser     varchar(50)                        null comment '浏览器',
    os          varchar(50)                        null comment '操作系统',
    user_agent  varchar(512)                       null comment 'User-Agent'
)
    comment '登陆日志表';

