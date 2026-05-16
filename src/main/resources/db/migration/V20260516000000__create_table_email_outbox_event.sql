create table if not exists email_outbox_event (
    id bigserial primary key,
    email varchar(255) not null,
    html_content text not null,
    email_send_type varchar(255) not null,
    publish_status varchar(255) not null,
    published_at timestamp(6),
    attempt_count integer not null default 0,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index if not exists idx_email_outbox_event_relay
    on email_outbox_event (publish_status, attempt_count, created_at);
