create table if not exists article
(
    id          bigint auto_increment comment '文章ID'
        primary key,
    title       varchar(255)                       not null comment '文章标题',
    content     text                               not null comment '文章内容',
    author      varchar(100)                       null comment '作者',
    create_time datetime default CURRENT_TIMESTAMP null comment '创建时间',
    update_time datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted     tinyint  default 0                 not null comment '逻辑删除：0未删除 1已删除',
    status      tinyint  default 0                 not null comment '文章状态：0草稿 1已发布',
    view_count  bigint   default 0                 null comment '浏览量',
    like_count  int      default 0                 not null comment '点赞数量',
    user_id     bigint                             null comment '作者ID'
)
    comment '文章表' charset = utf8mb4;

