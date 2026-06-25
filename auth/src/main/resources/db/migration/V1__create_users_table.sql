CREATE TABLE users (
    id                  UUID PRIMARY KEY,
    email               VARCHAR(255) NOT NULL,
    name                VARCHAR(255) NOT NULL,
    password_hash       VARCHAR(255) NOT NULL,
    reading_level       VARCHAR(255) NOT NULL,
    profile_picture_url VARCHAR(255),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX ux_users_email ON users (LOWER(email));