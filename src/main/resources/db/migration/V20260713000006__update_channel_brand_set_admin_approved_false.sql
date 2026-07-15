update channel_brand
set admin_approved = false
where admin_approved is null;
