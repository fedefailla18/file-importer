--liquibase formatted sql
--changeset ffailla:2024-07-20-015-adding-transaction-portfolio.sql
--comment: adding portfolio to transaction;

ALTER TABLE transactions
    ADD COLUMN portfolio_id  uuid;
ALTER TABLE transactions
    ADD FOREIGN KEY (portfolio_id) REFERENCES portfolio (id);

UPDATE transactions
set portfolio_id = '1c56e6eb-c0eb-46a6-a4a6-acd770d08c77'
