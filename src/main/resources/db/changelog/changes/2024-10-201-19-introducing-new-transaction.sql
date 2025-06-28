--liquibase formatted sql
--changeset ffailla:2024-10-201-19-introducing-new-transaction.sql
--comment:

-- Step 1: Add new columns with the correct naming and types
ALTER TABLE transactions
    ADD COLUMN new_paid_amount numeric(22, 13),
    ADD COLUMN new_paid_with VARCHAR(255),
    ADD COLUMN new_date_utc TIMESTAMP,
    ADD COLUMN new_side VARCHAR(12),
    ADD COLUMN new_pair VARCHAR(12),
    ADD COLUMN new_price numeric(22, 13),
    ADD COLUMN new_executed numeric(22, 13),
    ADD COLUMN processed BOOLEAN DEFAULT FALSE,
    ADD COLUMN last_processed_at TIMESTAMP;

-- Step 2: Migrate data from old columns to the new columns
UPDATE transactions
SET new_paid_amount = payed_amount,
    new_paid_with = payed_with,
    new_date_utc = date_utc,
    new_side = side,
    new_pair = pair,
    new_price = price,
    new_executed = executed;

-- Step 3: Drop the old columns after data migration
ALTER TABLE transactions
    DROP COLUMN payed_amount,
    DROP COLUMN payed_with,
    DROP COLUMN date_utc,
    DROP COLUMN side,
    DROP COLUMN pair,
    DROP COLUMN price,
    DROP COLUMN executed;

-- Step 4: Rename the new columns to match the original names
ALTER TABLE transactions
    RENAME COLUMN new_paid_amount TO paid_amount;
ALTER TABLE transactions
    RENAME COLUMN new_paid_with TO paid_with;
ALTER TABLE transactions
    RENAME COLUMN new_date_utc TO date_utc;
ALTER TABLE transactions
    RENAME COLUMN new_side TO side;
ALTER TABLE transactions
    RENAME COLUMN new_pair TO pair;
ALTER TABLE transactions
    RENAME COLUMN new_price TO price;
ALTER TABLE transactions
    RENAME COLUMN new_executed TO executed;
