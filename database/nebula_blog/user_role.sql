create table if not exists user_role
(
    user_id bigint not null,
    role_id bigint not null,
    primary key (user_id, role_id),
    constraint fk_role
        foreign key (role_id) references role (id),
    constraint fk_user
        foreign key (user_id) references user (id)
);

create index idx_role_id
    on user_role (role_id);

create index idx_user_id
    on user_role (user_id);

