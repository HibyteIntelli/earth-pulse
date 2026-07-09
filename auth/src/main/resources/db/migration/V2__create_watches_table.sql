CREATE TABLE watches (
    id            UUID PRIMARY KEY,
    user_id       UUID NOT NULL
        CONSTRAINT fk_watches_users REFERENCES users (id),
    name          VARCHAR(255),
    min_lat       DOUBLE PRECISION NOT NULL,
    max_lat       DOUBLE PRECISION NOT NULL,
    min_lon       DOUBLE PRECISION NOT NULL,
    max_lon       DOUBLE PRECISION NOT NULL,
    digest_mode   VARCHAR(255) NOT NULL DEFAULT 'IMMEDIATE'
        CONSTRAINT ck_watches_digest_mode CHECK (digest_mode IN ('IMMEDIATE', 'DAILY')),
    reading_level VARCHAR(255) NOT NULL DEFAULT 'DEFAULT'
        CONSTRAINT ck_watches_reading_level CHECK (reading_level IN ('DEFAULT', 'SIMPLIFIED')),
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_watches_user_id ON watches (user_id);

CREATE TABLE watch_categories (
    watch_id UUID NOT NULL
        CONSTRAINT fk_watch_categories_watches REFERENCES watches (id) ON DELETE CASCADE,
    category VARCHAR(255) NOT NULL
        CONSTRAINT ck_watch_categories_category CHECK (category IN (
            'DROUGHT', 'DUST_HAZE', 'EARTHQUAKES', 'FLOODS', 'LANDSLIDES', 'MANMADE',
            'SEA_LAKE_ICE', 'SEVERE_STORMS', 'SNOW', 'TEMP_EXTREMES', 'VOLCANOES',
            'WATER_COLOR', 'WILDFIRES'
        ))
);

CREATE INDEX idx_watch_categories_watch_id ON watch_categories (watch_id);