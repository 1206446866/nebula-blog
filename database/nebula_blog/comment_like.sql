create table if not exists comment_like
(
    id          bigint auto_increment comment '主键ID'
        primary key,
    comment_id  bigint                             not null comment '评论ID',
    user_id     bigint                             not null comment '用户ID',
    create_time datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    constraint uk_comment_user
        unique (comment_id, user_id)
)
    comment '评论点赞关系表';

create index idx_comment_id
    on comment_like (comment_id);

create index idx_user_id
    on comment_like (user_id);

