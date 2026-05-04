alter table video_stats
alter column view_count set default 0,
alter column view_count set not null;
