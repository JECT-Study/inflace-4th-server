create table if not exists video (
    id bigserial primary key,
    channel_id bigint not null,
    category_id integer,
    youtube_video_id varchar(255) not null,
    title varchar(255),
    description text,
    thumbnail_url text,
    duration_seconds integer,
    is_short boolean not null default false,
    is_advertisement boolean not null default false,
    published_at timestamp(6),
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint fk_video_channel
        foreign key (channel_id) references channel (id),
    constraint uk_video_youtube_video
        unique (youtube_video_id)
);
