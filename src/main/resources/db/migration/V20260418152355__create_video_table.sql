create table if not exists video (
    video_id bigserial primary key,
    channel_id bigint not null,
    title varchar(255),
    thumbnail_url varchar(255),
    duration double precision,
    is_short boolean not null,
    rising_score double precision,
    published_at timestamp(6),
    hashtags text[],
    category text[],
    youtube_video_id varchar(255),
    video_url varchar(255),
    description varchar(255),
    is_advertisement boolean not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint fk_video_channel foreign key (channel_id) references channel (channel_id)
);
