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


Certainly! Here are the SQL scripts for creating tables for your Role and User entities:

SQL Script for Creating the roles Table:
sql
Copy code
CREATE TABLE roles (
                       id SERIAL PRIMARY KEY,
                       name VARCHAR(20) NOT NULL
);

-- Add additional SQL statements for indexes or constraints if needed
SQL Script for Creating the users Table:
sql
Copy code
CREATE TABLE users (
                       id BIGINT SERIAL PRIMARY KEY,
                       username VARCHAR(255) NOT NULL,
                       email VARCHAR(255) NOT NULL,
                       password VARCHAR(255) NOT NULL
);

-- Add additional SQL statements for indexes or constraints if needed

CREATE TABLE user_roles (
                            user_id BIGINT NOT NULL,
                            role_id INTEGER NOT NULL,
                            PRIMARY KEY (user_id, role_id),
                            FOREIGN KEY (user_id) REFERENCES users (id),
                            FOREIGN KEY (role_id) REFERENCES roles (id)
);
These SQL scripts define the tables and their columns for the Role and User entities. You can further customize them as per your requirements by adding indexes, constraints, or any other database-specific configurations.

You can execute these SQL scripts using a database management tool or include them in your Liquibase change log XML files to ensure version-controlled database schema changes.