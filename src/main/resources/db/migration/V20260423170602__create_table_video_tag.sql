create table if not exists video_tag (
    id bigserial primary key,
    video_id bigint not null,
    tag varchar(255) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint fk_video_tag_video
        foreign key (video_id) references video (id),
    constraint uk_video_tag
        unique (video_id, tag)
);
