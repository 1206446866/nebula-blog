create table if not exists role
(
    id          bigint auto_increment
        primary key,
    name        varchar(50)                          not null comment '角色名称',
    description varchar(255)                         null comment '角色描述',
    create_time datetime   default CURRENT_TIMESTAMP null comment '创建时间',
    update_time datetime   default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted     tinyint(1) default 0                 not null comment '逻辑删除(0-未删 1-已删)'
);

create index idx_role_deleted
    on role (deleted);

