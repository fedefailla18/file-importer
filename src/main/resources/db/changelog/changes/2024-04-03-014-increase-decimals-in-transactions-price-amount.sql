--liquibase formatted sql
--changeset ffailla:2024-04-03-014-increase-decimals-in-transactions-price-amount.sql
--comment: modifying portfolio holding price_in_btc;

ALTER TABLE transactions
    ADD COLUMN price_new numeric(22, 13);
UPDATE transactions
SET price_new = price;
ALTER TABLE transactions
    DROP COLUMN price;
ALTER TABLE transactions
    RENAME COLUMN price_new TO price;

ALTER TABLE transactions
    ADD COLUMN payed_amount_new numeric(22, 13);
UPDATE transactions
SET payed_amount_new = payed_amount;
ALTER TABLE transactions
    DROP COLUMN payed_amount;
ALTER TABLE transactions
    RENAME COLUMN payed_amount_new TO payed_amount;

ALTER TABLE transactions
    ADD COLUMN fee_amount_new numeric(22, 13);
UPDATE transactions
SET fee_amount_new = fee_amount;
ALTER TABLE transactions
    DROP COLUMN fee_amount;
ALTER TABLE transactions
    RENAME COLUMN fee_amount_new TO fee_amount;


ALTER TABLE transactions
    ADD COLUMN executed_new numeric(22, 13);
UPDATE transactions
SET executed_new = executed;
ALTER TABLE transactions
    DROP COLUMN executed;
ALTER TABLE transactions
    RENAME COLUMN executed_new TO executed;

ALTER TABLE transactions
    ALTER COLUMN created SET DEFAULT now(),
    ALTER COLUMN modified SET DEFAULT now();
