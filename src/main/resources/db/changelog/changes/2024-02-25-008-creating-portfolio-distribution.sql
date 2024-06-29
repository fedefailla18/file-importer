--liquibase formatted sql
--changeset ffailla:2024-02-25-008-creating-portfolio-distribution.sql
--comment: altering symbol table;

ALTER TABLE symbol
    DROP COLUMN symbol;
ALTER TABLE symbol
    ADD COLUMN symbol varchar(255) unique;

--comment: creating portfolio;
CREATE TABLE portfolio
(
    id            uuid primary key DEFAULT gen_random_uuid(),
    name          varchar(255) not null,
    symbol        varchar(8),
    creation_date timestamp,
    created       timestamp,
    created_by    varchar(255),
    modified      timestamp,
    modified_by   varchar(255)
);

--comment: creating holdings;
CREATE TABLE portfolio_holding
(
    id            uuid primary key DEFAULT gen_random_uuid(),
    portfolio_id  uuid,
    symbol        varchar(8) not null,
    amount        numeric(12, 5),
    percent       numeric(12, 5),
    created       timestamp,
    created_by    varchar(255),
    modified      timestamp,
    modified_by   varchar(255),
    FOREIGN KEY (portfolio_id) REFERENCES portfolio (id)
);

