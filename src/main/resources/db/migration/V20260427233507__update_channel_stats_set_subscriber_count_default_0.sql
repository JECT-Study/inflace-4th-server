update channel_stats
set subscriber_count = 0
where subscriber_count is null;
