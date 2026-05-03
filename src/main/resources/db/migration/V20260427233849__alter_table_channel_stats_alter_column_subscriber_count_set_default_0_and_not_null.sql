alter table channel_stats
alter column subscriber_count set default 0,
alter column subscriber_count set not null;
