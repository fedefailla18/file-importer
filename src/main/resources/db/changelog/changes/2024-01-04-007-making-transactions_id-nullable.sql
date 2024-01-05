--liquibase formatted sql
--changeset ffailla:2024-01-04-007-making-transactions_id-nullable.sql
--comment: altering transactions.idCREATE SEQUENCE table_name_id_seq;
CREATE SEQUENCE transactions_id_seq;

ALTER TABLE transactions
    DROP COLUMN id;
ALTER TABLE transactions
    ADD COLUMN id integer DEFAULT nextval('transactions_id_seq');

ALTER SEQUENCE transactions_id_seq
    OWNED BY transactions.id;


