create table if not exists channel_stats (
    id bigserial primary key,
    channel_id bigint not null,
    subscriber_count bigint,
    total_view_count bigint,
    total_video_count bigint,
    recent_upload_count_30d integer,
    avg_views_recent_n double precision,
    avg_engagement_rate_recent_n double precision,
    collected_at timestamp not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint fk_channel_stats_channel
        foreign key (channel_id) references channel (id),
    constraint uk_channel_stats_channel
        unique (channel_id)
);
