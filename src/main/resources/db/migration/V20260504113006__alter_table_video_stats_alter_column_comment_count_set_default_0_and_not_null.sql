alter table video_stats
alter column comment_count set default 0,
alter column comment_count set not null;
