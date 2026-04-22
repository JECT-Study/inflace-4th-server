create table if not exists channel_stats_history (
    channel_stats_history_id bigserial primary key,
    channel_id bigint not null,
    subscriber_count bigint,
    recorded_date timestamp(6),
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint fk_channel_stats_history_channel foreign key (channel_id) references channel (channel_id)
);
