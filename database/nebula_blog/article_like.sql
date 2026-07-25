create table if not exists article_like
(
    id          bigint auto_increment comment '主键ID'
        primary key,
    article_id  bigint                             not null comment '文章ID',
    user_id     bigint                             not null comment '用户ID',
    create_time datetime default CURRENT_TIMESTAMP not null comment '点赞时间',
    constraint uk_article_user
        unique (article_id, user_id)
)
    comment '文章点赞关系表';

create index idx_article_id
    on article_like (article_id);

create index idx_user_id
    on article_like (user_id);

