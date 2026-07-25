create table if not exists permission
(
    id          bigint auto_increment
        primary key,
    name        varchar(50)                        not null comment '权限名称',
    description varchar(255)                       null comment '权限描述',
    create_time datetime default CURRENT_TIMESTAMP null comment '创建时间',
    update_time datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    status      tinyint  default 0                 null comment '状态',
    parent_id   bigint   default 0                 null comment '父权限ID，0为根节点'
);

