--liquibase formatted sql
--changeset ffailla:2024-03-06-011-modifying-portfolio-holding-amount.sql
--comment: modifying portfolio holding amount;

ALTER TABLE portfolio_holding
    ADD COLUMN amount_new numeric(22, 5);
UPDATE portfolio_holding
SET amount_new = amount;
ALTER TABLE portfolio_holding
    DROP COLUMN amount;
ALTER TABLE portfolio_holding
    RENAME COLUMN amount_new TO amount;

--comment: modifying symbol
ALTER TABLE portfolio_holding
    ADD COLUMN symbol_new varchar(12);
UPDATE portfolio_holding
SET symbol_new = symbol;
ALTER TABLE portfolio_holding
    DROP COLUMN symbol;
ALTER TABLE portfolio_holding
    RENAME COLUMN symbol_new TO symbol;

--comment: modifying symbol in symbol table
ALTER TABLE symbol
    ADD COLUMN symbol_new varchar(12);
UPDATE symbol
SET symbol_new = symbol;
ALTER TABLE symbol
    DROP COLUMN symbol;
ALTER TABLE symbol
    RENAME COLUMN symbol_new TO symbol;
