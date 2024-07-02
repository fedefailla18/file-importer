
-- Create a new database with the user as the owner
CREATE DATABASE importer_database OWNER root;
-- Create a new schema and set the user as the owner
CREATE SCHEMA file_importer_schema AUTHORIZATION root;

-- Create the event table inside the new schema
BEGIN;
CREATE TABLE IF NOT EXISTS
    file_importer_schema.event (
        id CHAR(26) NOT NULL CHECK (CHAR_LENGTH(id) = 26) PRIMARY KEY,
          aggregate_id CHAR(26) NOT NULL CHECK (CHAR_LENGTH(aggregate_id) = 26),
          event_data JSON NOT NULL,
          version INT,
          UNIQUE(aggregate_id, version)
);
CREATE INDEX idx_event_aggregate_id ON file_importer_schema.event (aggregate_id);
COMMIT;
