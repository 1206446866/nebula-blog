create table if not exists article_category
(
    article_id  bigint not null comment '文章ID',
    category_id bigint not null comment '分类ID',
    primary key (article_id, category_id)
);

