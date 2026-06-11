create index if not exists idx_video_advertisement_channel
    on video (channel_id)
    where is_advertisement = true;
