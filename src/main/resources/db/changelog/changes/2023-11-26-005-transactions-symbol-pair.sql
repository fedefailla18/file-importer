--liquibase formatted sql
--changeset ffailla:2023-11-26-005-transactions-symbol-pair.sql
--comment: transactions-symbol-pair.
ALTER TABLE transactions
    drop column symbolpair;
ALTER TABLE transactions
    add column symbol_pair varchar(12);

