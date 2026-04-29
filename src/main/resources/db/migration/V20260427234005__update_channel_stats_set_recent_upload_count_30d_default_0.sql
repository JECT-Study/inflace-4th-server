update channel_stats
set recent_upload_count_30d = 0
where recent_upload_count_30d is null;
