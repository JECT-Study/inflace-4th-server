create table if not exists channel (
    channel_id bigserial primary key,
    user_id uuid not null,
    name varchar(255),
    youtube_channel_id varchar(255),
    category text[],
    channel_handle varchar(255),
    entered_at timestamp(6),
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint fk_channel_user foreign key (user_id) references users (user_id)
);
