create table if not exists channel (
    id bigserial primary key,
    user_id uuid,
    youtube_channel_id varchar(255) not null,
    name varchar(255),
    channel_handle varchar(255),
    profile_image_url text,
    uploads_playlist_id varchar(255),
    youtube_published_at timestamp,
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint fk_channel_user
        foreign key (user_id) references users (user_id),
    constraint uk_channel_user_youtube
        unique (user_id, youtube_channel_id)
);
