--liquibase formatted sql
--changeset ffailla:2024-02-26-010-adding-relevant-fields-from-CoinInformationResponse.sql
--comment: altering portfolio_holding table to have different expressed amount ;

ALTER TABLE portfolio_holding
    ADD COLUMN total_amount_bought numeric(22, 5);
ALTER TABLE portfolio_holding
    ADD COLUMN total_amount_sold numeric(22, 5);
ALTER TABLE portfolio_holding
    ADD COLUMN stable_total_cost numeric(22, 5);
ALTER TABLE portfolio_holding
    ADD COLUMN current_position_in_usdt numeric(22, 5);
