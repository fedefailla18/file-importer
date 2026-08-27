--liquibase formatted sql
--changeset ffailla:2026-05-01-042-add-external-id-to-transactions
--comment: Add external_id and exchange_name to transactions for deduplication

ALTER TABLE file_importer_schema.transactions
    ADD COLUMN external_id VARCHAR(100),
    ADD COLUMN exchange_name VARCHAR(20);

ALTER TABLE file_importer_schema.transactions
    ADD CONSTRAINT uk_portfolio_exchange_extid UNIQUE (portfolio_id, exchange_name, external_id);

CREATE INDEX idx_transactions_exchange_extid ON file_importer_schema.transactions(exchange_name, external_id);
