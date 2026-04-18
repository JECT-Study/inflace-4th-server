create table users_need (
    need_id bigserial primary key,
    need varchar(255),
    user_id uuid,
    constraint fk_users_need_user foreign key (user_id) references users (user_id)
);
