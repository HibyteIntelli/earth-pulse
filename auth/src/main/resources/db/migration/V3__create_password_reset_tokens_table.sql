CREATE TABLE password_reset_tokens (
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL
        CONSTRAINT fk_password_reset_tokens_users REFERENCES users (id) ON DELETE CASCADE,
    token      VARCHAR(36) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used       BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX ux_password_reset_tokens_token ON password_reset_tokens (token);