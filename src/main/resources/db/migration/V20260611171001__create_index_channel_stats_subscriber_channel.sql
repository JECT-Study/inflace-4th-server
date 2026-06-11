create index if not exists idx_channel_stats_subscriber_channel
    on channel_stats (subscriber_count desc, channel_id desc);
