create table if not exists channel_stats_history (
    id bigserial primary key,
    channel_id bigint not null,
    snapshot_date date not null,
    subscriber_count bigint,
    total_view_count bigint,
    video_count bigint,
    collected_at timestamp not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint fk_channel_stats_history_channel
        foreign key (channel_id) references channel (id),
    constraint uk_channel_stats_history_channel_date
        unique (channel_id, snapshot_date)
);
