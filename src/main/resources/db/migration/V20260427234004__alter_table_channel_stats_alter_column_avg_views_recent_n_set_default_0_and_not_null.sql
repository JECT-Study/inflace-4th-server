alter table channel_stats
alter column avg_views_recent_n set default 0.0,
alter column avg_views_recent_n set not null;
