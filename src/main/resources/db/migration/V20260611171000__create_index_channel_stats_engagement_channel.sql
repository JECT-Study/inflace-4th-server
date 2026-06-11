create index if not exists idx_channel_stats_engagement_channel
    on channel_stats (avg_engagement_rate_recent desc, channel_id desc);
