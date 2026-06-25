create index if not exists idx_brand_lower_name_bigm
    on brand using gin (lower(name) gin_bigm_ops);

-- 운영 환경에서 쓰기 차단을 피하려면 위 구문 대신 아래 인덱스를 수동으로 생성한다.
-- create index concurrently if not exists idx_brand_lower_name_bigm
--     on brand using gin (lower(name) gin_bigm_ops);
