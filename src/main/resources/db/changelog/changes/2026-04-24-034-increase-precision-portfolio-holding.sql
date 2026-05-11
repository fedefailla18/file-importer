--liquibase formatted sql
--changeset ffailla:2026-04-24-034-increase-precision-portfolio-holding.sql
--comment: increase precision of numeric columns in portfolio_holding to prevent DataIntegrityViolationException;

ALTER TABLE portfolio_holding ALTER COLUMN amount TYPE numeric(38, 18);
ALTER TABLE portfolio_holding ALTER COLUMN amount_in_btc TYPE numeric(38, 18);
ALTER TABLE portfolio_holding ALTER COLUMN amount_in_usdt TYPE numeric(38, 18);
ALTER TABLE portfolio_holding ALTER COLUMN price_in_btc TYPE numeric(38, 18);
ALTER TABLE portfolio_holding ALTER COLUMN price_in_usdt TYPE numeric(38, 18);
ALTER TABLE portfolio_holding ALTER COLUMN percent TYPE numeric(38, 18);
ALTER TABLE portfolio_holding ALTER COLUMN total_amount_bought TYPE numeric(38, 18);
ALTER TABLE portfolio_holding ALTER COLUMN total_amount_sold TYPE numeric(38, 18);
ALTER TABLE portfolio_holding ALTER COLUMN stable_total_cost TYPE numeric(38, 18);
ALTER TABLE portfolio_holding ALTER COLUMN current_position_in_usdt TYPE numeric(38, 18);
ALTER TABLE portfolio_holding ALTER COLUMN total_realized_profit_usdt TYPE numeric(38, 18);
