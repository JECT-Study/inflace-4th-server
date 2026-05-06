CREATE TABLE IF NOT EXISTS brand_alias (
    id BIGSERIAL PRIMARY KEY,
    brand_id BIGINT NOT NULL,
    alias VARCHAR(255) NOT NULL,
    CONSTRAINT fk_brand_alias_brand FOREIGN KEY (brand_id) REFERENCES brand (id),
    CONSTRAINT uk_brand_alias UNIQUE (brand_id, alias)
);
