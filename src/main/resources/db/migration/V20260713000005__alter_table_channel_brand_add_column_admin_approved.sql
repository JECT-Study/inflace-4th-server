alter table channel_brand
    add column if not exists admin_approved boolean;
