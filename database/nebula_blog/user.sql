create table if not exists user
(
    id          bigint auto_increment
        primary key,
    username    varchar(50)       not null,
    password    varchar(100)      not null,
    status      tinyint default 0 null comment '状态',
    deleted     tinyint default 0 not null comment '逻辑删除 0未删除 1已删除',
    create_time datetime          null comment '创建时间',
    update_time datetime          null comment '更新时间',
    avatar      varchar(255)      null comment '头像URL',
    nid         varchar(64)       not null comment '登录账号（唯一标识）',
    email       varchar(128)      null comment '邮箱',
    phone       varchar(20)       null comment '手机号',
    constraint idx_nid
        unique (nid),
    constraint uk_user_email
        unique (email),
    constraint uk_user_phone
        unique (phone)
)
    charset = utf8mb4;

