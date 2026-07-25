create table if not exists category
(
    id          bigint auto_increment comment '分类ID'
        primary key,
    name        varchar(50)       not null comment '分类名称',
    description varchar(255)      null comment '分类描述',
    create_time datetime          null comment '创建时间',
    update_time datetime          null comment '更新时间',
    deleted     tinyint default 0 not null comment '是否删除 0否 1是'
);

