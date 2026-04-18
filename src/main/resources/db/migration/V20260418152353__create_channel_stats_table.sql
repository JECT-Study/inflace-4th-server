create table channel_stats (
    channel_stats_id bigserial primary key,
    channel_id bigint not null,
    subscriber_count bigint,
    total_view_count bigint,
    subscriber_view_count bigint,
    avg_engagement_rate double precision,
    audience_gender jsonb,
    audience_age jsonb,
    audience_country jsonb,
    collected_at timestamp(6),
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint fk_channel_stats_channel foreign key (channel_id) references channel (channel_id),
    constraint uk_channel_stats_channel_id unique (channel_id)
);
