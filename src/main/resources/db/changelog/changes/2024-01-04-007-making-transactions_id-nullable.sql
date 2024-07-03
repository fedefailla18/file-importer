--liquibase formatted sql
--changeset ffailla:2024-01-04-007-making-transactions_id-nullable.sql
--comment: altering transactions.idCREATE SEQUENCE table_name_id_seq;
-- Create the sequence if it does not exist
CREATE SEQUENCE IF NOT EXISTS transact_id_seq;

-- Drop and add the column
ALTER TABLE transactions
    DROP COLUMN IF EXISTS id;

ALTER TABLE transactions
    ADD COLUMN id INTEGER DEFAULT nextval('transact_id_seq');

-- Set ownership of the sequence
ALTER SEQUENCE transact_id_seq
    OWNED BY transactions.id;
