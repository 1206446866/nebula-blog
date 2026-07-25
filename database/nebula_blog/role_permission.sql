create table if not exists role_permission
(
    id            bigint auto_increment
        primary key,
    role_id       bigint                             not null comment '角色ID',
    permission_id bigint                             not null comment '权限ID',
    create_time   datetime default CURRENT_TIMESTAMP null comment '创建时间',
    constraint uk_role_permission
        unique (role_id, permission_id)
);

create index idx_permission_id
    on role_permission (permission_id);

create index idx_role_id
    on role_permission (role_id);

