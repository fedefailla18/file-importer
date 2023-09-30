--liquibase formatted sql
--changeset ffailla:2023-09-30-003-recording-transactions.sql
--comment: creating table to keep transactions.
CREATE TABLE transactions
(
    id            uuid primary key DEFAULT gen_random_uuid(),
    name          varchar(255),
    date_utc      timestamp,
    pair          varchar(12),
    side          varchar(12),
    symbol        varchar(8),
    symbolPair    varchar(8),
    price         numeric(12, 5),
    executed      numeric(12, 5),
    amount        numeric(12, 5),
    fee           numeric(12, 5),
    created       timestamp,
    created_by    varchar(255),
    modified      timestamp,
    modified_by   varchar(255)
);

--comment: creating price_history table
CREATE TABLE price_history
(
    id            uuid primary key DEFAULT gen_random_uuid(),
    name          varchar(255),
    time          timestamp,
    pair          varchar(12),
    symbol        varchar(8),
    symbolPair    varchar(8),
    high          numeric(15, 5),
    low           numeric(15, 5),
    open          numeric(15, 5),
    close         numeric(15, 5),
    volumeto      numeric(30, 5),
    volumefrom    numeric(30, 5),
    created       timestamp,
    created_by    varchar(255),
    modified      timestamp,
    modified_by   varchar(255)
);

