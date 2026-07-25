create table if not exists comment
(
    id          bigint auto_increment comment '评论ID'
        primary key,
    article_id  bigint            not null comment '文章ID',
    user_id     bigint            null comment '用户ID',
    parent_id   bigint  default 0 null comment '父评论ID',
    content     text              not null comment '评论内容',
    create_time datetime          null comment '创建时间',
    update_time datetime          null comment '更新时间',
    status      tinyint default 0 null comment '状态',
    like_count  int     default 0 not null comment '评论点赞数'
);

