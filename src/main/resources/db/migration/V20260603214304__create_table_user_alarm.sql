create table user_alarm (
    user_alarm_id bigserial primary key,
    user_id uuid not null,
    alarm_type varchar(50) not null,
    enabled boolean not null default true,
    created_at timestamp not null,
    updated_at timestamp not null,

    constraint fk_user_alarm_user
        foreign key (user_id) references users(user_id),

    constraint uk_user_alarm_user_alarm_type
        unique (user_id, alarm_type)
);