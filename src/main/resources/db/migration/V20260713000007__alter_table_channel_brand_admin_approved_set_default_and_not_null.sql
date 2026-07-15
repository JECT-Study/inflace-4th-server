alter table channel_brand
    alter column admin_approved set default false,
    alter column admin_approved set not null;
