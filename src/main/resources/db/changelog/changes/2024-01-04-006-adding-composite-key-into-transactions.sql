--liquibase formatted sql
--changeset ffailla:2024-01-04-006-adding-composite-key-into-transactions.sql
--comment: creating table to keep transactions with composite key

DROP TABLE transactions;

CREATE TABLE transactions
(
    id            SERIAL,
    date_utc      timestamp NOT NULL,
    side          varchar(12) NOT NULL,
    pair          varchar(12) NOT NULL,
    symbol        varchar(8),
    payed_with    varchar(12), -- symbol-pair
    price         numeric(12, 5) NOT NULL,
    executed      numeric(20, 5) NOT NULL, -- the amount of symbol bought
    payed_amount  numeric(12, 5),
    fee           varchar(12),
    fee_amount    numeric(12, 5),
    fee_symbol    varchar(8),
    created       timestamp,
    created_by    varchar(255),
    modified      timestamp,
    modified_by   varchar(255),
    CONSTRAINT ck_transactions
    PRIMARY KEY (date_utc, side, pair, price, executed)
);
