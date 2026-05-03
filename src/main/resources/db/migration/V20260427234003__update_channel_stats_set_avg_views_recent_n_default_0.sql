update channel_stats
set avg_views_recent_n = 0.0
where avg_views_recent_n is null;
