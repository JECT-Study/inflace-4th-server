alter table channel_stats
alter column recent_upload_count_30d set default 0,
alter column recent_upload_count_30d set not null;
