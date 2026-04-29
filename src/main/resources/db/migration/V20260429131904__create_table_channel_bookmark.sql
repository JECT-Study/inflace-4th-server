create table if not exists channel_bookmark (
    id bigserial primary key,
    channel_id bigint not null,
    user_id uuid not null,
    constraint fk_channel_bookmark_channel
        foreign key (channel_id) references channel (id),
    constraint fk_channel_bookmark_user
        foreign key (user_id) references users (user_id),
    constraint uk_channel_bookmark_channel_user
        unique (channel_id, user_id)
);
