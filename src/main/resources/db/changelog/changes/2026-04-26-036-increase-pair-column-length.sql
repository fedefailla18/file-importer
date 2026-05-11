--liquibase formatted sql
--changeset ffailla:2026-04-26-036-increase-pair-column-length
--comment: Increase transactions.pair column from VARCHAR(12) to VARCHAR(32) to support deposits/withdrawals with EXTERNAL suffix and longer trading pairs

ALTER TABLE file_importer_schema.transactions
    ALTER COLUMN pair TYPE VARCHAR(32);
