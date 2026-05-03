update channel_stats
set avg_engagement_rate_recent_n = 0.0
where avg_engagement_rate_recent_n is null;
