--liquibase formatted sql
--changeset ffailla:2024-04-02-013-increase-decimals-in-holdings-price-amount.sql
--comment: modifying portfolio holding price_in_btc;

ALTER TABLE portfolio_holding
    ADD COLUMN price_in_btc_new numeric(9, 9);
UPDATE portfolio_holding
SET price_in_btc_new = price_in_btc;
ALTER TABLE portfolio_holding
    DROP COLUMN price_in_btc;
ALTER TABLE portfolio_holding
    RENAME COLUMN price_in_btc_new TO price_in_btc;