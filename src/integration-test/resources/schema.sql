CREATE SCHEMA IF NOT EXISTS file_importer_schema;

-- Create the transactions table
CREATE TABLE transactions
(
    id            SERIAL,
    date_utc      timestamp NOT NULL,
    side          varchar(12) NOT NULL,
    pair          varchar(12) NOT NULL,
    symbol        varchar(8),
    payed_with    varchar(12), -- symbol-pair
    price         numeric(13, 13) NOT NULL,
    executed      numeric(20, 20) NOT NULL, -- the amount of symbol bought
    payed_amount  numeric(13, 13),
    fee           varchar(12),
    fee_amount    numeric(13, 13),
    fee_symbol    varchar(8),
    created       timestamp,
    created_by    varchar(255),
    modified      timestamp,
    modified_by   varchar(255),
    portfolio_id  uuid,
    processed     boolean DEFAULT false,
    last_processed_at timestamp,
    CONSTRAINT ck_transactions
    PRIMARY KEY (date_utc, side, pair, price, executed)
);

-- Create the sequence for transactions.id
CREATE SEQUENCE transactions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- Set ownership of the sequence
ALTER SEQUENCE transactions_id_seq
    OWNED BY transactions.id;

-- Set the default value for transactions.id
ALTER TABLE transactions ALTER COLUMN id SET DEFAULT nextval('transactions_id_seq');

-- Create the symbol table
CREATE TABLE symbol
(
    id            uuid primary key DEFAULT gen_random_uuid(),
    name          varchar(255),
    symbol        varchar(8),
    creation_date timestamp,
    created       timestamp,
    created_by    varchar(255),
    modified      timestamp,
    modified_by   varchar(255)
);

-- Create the portfolio table
CREATE TABLE portfolio
(
    id            uuid primary key DEFAULT gen_random_uuid(),
    name          varchar(255),
    created       timestamp,
    created_by    varchar(255),
    modified      timestamp,
    modified_by   varchar(255)
);

-- Create the price_history table
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
