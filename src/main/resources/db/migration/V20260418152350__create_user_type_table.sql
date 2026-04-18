create table user_type (
    id bigserial primary key,
    role varchar(255) not null,
    user_id uuid,
    constraint fk_user_type_user foreign key (user_id) references users (user_id),
    constraint uk_user_type_user_id unique (user_id)
);
