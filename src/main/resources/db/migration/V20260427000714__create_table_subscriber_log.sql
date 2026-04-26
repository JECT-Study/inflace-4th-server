create table if not exists subscriber_log (
    subscriber_log_id bigserial primary key,
    channel_id bigint not null,
    subscriber_count bigint,
    subscribers_gained bigint,
    subscribers_lost bigint,
    recorded_date date not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint fk_subscriber_log_channel
        foreign key (channel_id) references channel (id),
    constraint uk_subscriber_log_channel_date
        unique (channel_id, recorded_date)
);