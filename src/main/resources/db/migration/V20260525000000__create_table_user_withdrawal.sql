CREATE TABLE IF NOT EXISTS user_withdrawal (
    id         BIGSERIAL PRIMARY KEY,
    user_id    UUID         NOT NULL,
    reason     VARCHAR(50)  NOT NULL,
    detail     TEXT,
    created_at TIMESTAMP    NOT NULL,
    CONSTRAINT fk_user_withdrawal_user
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
);
