create index if not exists idx_audience_retention_video_time_ratio
    on audience_retention (video_id, time_ratio);