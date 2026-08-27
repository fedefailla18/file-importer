--liquibase formatted sql
--changeset ffailla:2026-05-01-043-add-raw-response-table
--comment: Create table to store raw API responses from integrations

CREATE TABLE file_importer_schema.external_api_raw_response (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    exchange_name VARCHAR(20) NOT NULL,
    response_type VARCHAR(50) NOT NULL,
    external_id VARCHAR(100),
    raw_json TEXT NOT NULL,
    fetched_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_raw_response_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_raw_response_user_exchange ON file_importer_schema.external_api_raw_response(user_id, exchange_name);
