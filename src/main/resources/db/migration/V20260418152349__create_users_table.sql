create table if not exists users (
    user_id uuid primary key,
    name varchar(255),
    profile_image varchar(255),
    email varchar(255),
    provider_id varchar(255) not null,
    plan varchar(255),
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted_at timestamp,
    constraint uk_users_provider_id unique (provider_id)
);
