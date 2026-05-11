--liquibase formatted sql
--changeset ffailla:2026-05-01-041-price-history-index
--comment: Add unique constraint and covering index on price_history for performant lookups by (symbol, symbolpair, hour)

CREATE UNIQUE INDEX IF NOT EXISTS uidx_price_history_symbol_pair_hour
    ON file_importer_schema.price_history (symbol, symbolpair, DATE_TRUNC('hour', time));

CREATE INDEX IF NOT EXISTS idx_price_history_lookup
    ON file_importer_schema.price_history (symbol, symbolpair, time) INCLUDE (high);
