--liquibase formatted sql
--changeset ffailla:2024-03-06-012-adding-holding-prices.sql
--comment: adding portfolio holding prices;

ALTER TABLE portfolio_holding
    ADD COLUMN price_in_btc numeric(22, 5);

ALTER TABLE portfolio_holding
    ADD COLUMN price_in_usdt numeric(22, 5);
