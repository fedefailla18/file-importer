CREATE TABLE external_api_log (
    id UUID PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL,
    provider VARCHAR(50) NOT NULL,
    endpoint VARCHAR(255) NOT NULL,
    method VARCHAR(10) NOT NULL,
    request_params TEXT,
    response_status INT,
    response_body TEXT,
    used_weight INT,
    user_id UUID,
    CONSTRAINT fk_external_api_log_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_external_api_log_user_time ON external_api_log(user_id, timestamp);
