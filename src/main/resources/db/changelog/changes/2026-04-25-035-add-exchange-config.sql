CREATE TABLE user_exchange_config (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    exchange_name VARCHAR(50) NOT NULL,
    api_key VARCHAR(255) NOT NULL,
    api_secret VARCHAR(512) NOT NULL,
    last_sync_timestamp BIGINT,
    CONSTRAINT fk_user_exchange_config_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_user_exchange_config_user ON user_exchange_config(user_id);
