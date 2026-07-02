alter table channel_brand
    alter column ai_generated set default false,
    alter column ai_generated set not null;
