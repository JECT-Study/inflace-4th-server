create table if not exists youtube_category (
    id bigserial primary key,
    youtube_category_id integer not null,
    title varchar(255) not null,
    assignable boolean,
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint uk_youtube_category_youtube_category_id
        unique (youtube_category_id)
);
