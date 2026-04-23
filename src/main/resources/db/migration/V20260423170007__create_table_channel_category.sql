create table if not exists channel_category (
    id bigserial primary key,
    channel_id bigint not null,
    category_id bigint not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint fk_channel_category_channel
        foreign key (channel_id) references channel (id),
    constraint fk_channel_category_youtube_category
        foreign key (category_id) references youtube_category (id),
    constraint uk_channel_category
        unique (channel_id, category_id)
);
