CREATE TABLE binance_sync_progress (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    last_synced_order_id BIGINT,
    status VARCHAR(20) NOT NULL,
    last_sync_time TIMESTAMP,
    CONSTRAINT fk_binance_sync_progress_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_user_symbol UNIQUE (user_id, symbol)
);

CREATE TABLE binance_raw_order (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    order_id BIGINT NOT NULL,
    client_order_id VARCHAR(50),
    price DECIMAL(20, 10),
    orig_qty DECIMAL(20, 10),
    executed_qty DECIMAL(20, 10),
    status VARCHAR(20),
    side VARCHAR(10),
    type VARCHAR(20),
    order_time BIGINT,
    raw_response TEXT,
    CONSTRAINT fk_binance_raw_order_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_user_order_id UNIQUE (user_id, symbol, order_id)
);

CREATE INDEX idx_binance_raw_order_user_symbol ON binance_raw_order(user_id, symbol);
