alter table user_type add constraint uk_user_type_user_role unique (user_id, role);
