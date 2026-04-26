create table if not exists channel_analytics (
    channel_analytics_id bigserial primary key,
    channel_id bigint not null,
    start_date date not null,
    end_date date not null,
    collected_at timestamp not null,
    views bigint,
    subscriber_view_count bigint,
    non_subscriber_view_count bigint,
    watched_minutes bigint,
    average_view_duration_seconds integer,
    audience_gender jsonb,
    audience_age jsonb,
    audience_country jsonb,
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint fk_channel_analytics_channel
        foreign key (channel_id) references channel (id),
    constraint uk_channel_analytics_channel
        unique (channel_id)
);
