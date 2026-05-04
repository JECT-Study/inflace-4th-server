alter table video_stats
alter column outlier_score set default 0,
alter column outlier_score set not null;
