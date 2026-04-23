create table if not exists video_stats (
    id bigserial primary key,
    video_id bigint not null,
    view_count bigint,
    like_count bigint,
    comment_count bigint,
    vph double precision,
    outlier_score double precision,
    rising_score double precision,
    collected_at timestamp(6) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint fk_video_stats_video
        foreign key (video_id) references video (id),
    constraint uk_video_stats_video
        unique (video_id)
);
