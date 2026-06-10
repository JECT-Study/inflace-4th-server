create index concurrently if not exists idx_channel_lower_name_bigm
    on channel using gin (lower(name) gin_bigm_ops);
