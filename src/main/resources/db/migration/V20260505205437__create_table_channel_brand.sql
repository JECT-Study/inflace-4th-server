CREATE TABLE IF NOT EXISTS channel_brand
(
    id BIGSERIAL PRIMARY KEY,
    channel_id    BIGINT       NOT NULL,
    brand_id      BIGINT       NOT NULL,
    matched_alias VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT fk_channel_brand_channel FOREIGN KEY (channel_id) REFERENCES channel (id),
    CONSTRAINT fk_channel_brand_brand FOREIGN KEY (brand_id) REFERENCES brand (id),
    CONSTRAINT uk_channel_brand UNIQUE (channel_id, brand_id)
);
