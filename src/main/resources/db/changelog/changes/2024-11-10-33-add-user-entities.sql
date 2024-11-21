--liquibase formatted sql
--changeset ffailla:2024-11-10-33-add-user-entities.sql
--comment: Creating user table and updating related tables

DROP TABLE user_roles;
DROP TABLE users;

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    created TIMESTAMP,
    created_by VARCHAR(255),
    modified TIMESTAMP,
    modified_by VARCHAR(255)
);

CREATE TABLE user_roles (
    user_id uuid NOT NULL,
    role_id INTEGER NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users (id),
    FOREIGN KEY (role_id) REFERENCES roles (id),
    created TIMESTAMP DEFAULT NOW(),
    created_by VARCHAR(255),
    modified TIMESTAMP DEFAULT NOW(),
    modified_by VARCHAR(255)
);

ALTER TABLE portfolio
    ADD COLUMN user_id UUID,
    ADD CONSTRAINT fk_portfolio_user
        FOREIGN KEY (user_id)
            REFERENCES users(id);

INSERT INTO users (username, email, password, created)
VALUES ('default_user', 'default@example.com', 'change_me', CURRENT_TIMESTAMP);

UPDATE portfolio
SET user_id = (SELECT id FROM users WHERE username = 'default_user'),
    modified_by = '2024-10-22-020-add-user-entities.sql',
    modified = now()
WHERE user_id IS NULL;

ALTER TABLE portfolio
    ALTER COLUMN user_id SET NOT NULL;
