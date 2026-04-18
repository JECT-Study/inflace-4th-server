create table audience_retention (
    audience_retention_id bigserial primary key,
    video_id bigint not null,
    time_ratio double precision,
    retention_rate double precision,
    collected_at timestamp(6),
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint fk_audience_retention_video foreign key (video_id) references video (video_id)
);
