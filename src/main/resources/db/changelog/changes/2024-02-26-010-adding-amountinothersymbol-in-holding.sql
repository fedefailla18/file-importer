--liquibase formatted sql
--changeset ffailla:2024-02-26-010-adding-amountinothersymbol-in-holding.sql
--comment: altering portfolio_holding table to have different expressed amount ;

ALTER TABLE portfolio_holding
    ADD COLUMN amount_in_btc        numeric(12, 5);
ALTER TABLE portfolio_holding
    ADD COLUMN amount_in_usdt        numeric(12, 5);
