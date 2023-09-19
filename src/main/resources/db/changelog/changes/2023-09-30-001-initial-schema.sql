--liquibase formatted sql
--changeset ffailla:2023-09-30-001-initial-schema.sql
--comment: creating symbol table

CREATE TABLE symbol
(
    id uuid primary key,
    name varchar(255),
    symbol varchar(8),
    creation_date timestamp,
    created timestamp,
    created_by varchar(255),
    modified timestamp,
    modified_by varchar(255)
);