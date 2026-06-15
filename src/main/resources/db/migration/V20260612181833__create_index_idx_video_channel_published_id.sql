create index if not exists idx_video_channel_published_id
    on video (channel_id, published_at desc, id desc);