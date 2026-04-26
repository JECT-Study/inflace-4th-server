create table if not exists video_analytics (
    video_analytics_id bigserial primary key,
    video_id bigint not null,
    share_count bigint,
    ctr double precision,
    avg_watch_duration double precision,
    subscribers_gained bigint,
    unsubscribed_view_count bigint,
    average_view_percentage double precision,
    relative_retention_performance double precision,
    unsubscribed_viewer_percentage double precision,
    collected_at timestamp(6),
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint fk_video_analytics_video
        foreign key (video_id) references video (id),
    constraint uk_video_analytics_video
        unique (video_id)
);