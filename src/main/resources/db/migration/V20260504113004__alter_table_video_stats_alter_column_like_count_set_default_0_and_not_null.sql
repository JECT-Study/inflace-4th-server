alter table video_stats
alter column like_count set default 0,
alter column like_count set not null;
