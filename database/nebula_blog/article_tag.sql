create table if not exists article_tag
(
    article_id bigint not null comment '文章ID',
    tag_id     bigint not null comment '标签ID',
    primary key (article_id, tag_id)
);

