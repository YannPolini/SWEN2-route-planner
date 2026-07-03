CREATE TABLE IF NOT EXISTS app_user (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS tour (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    start_location VARCHAR(255) NOT NULL,
    end_location VARCHAR(255) NOT NULL,
    transport_type VARCHAR(255) NOT NULL,
    start_lat DOUBLE PRECISION,
    start_lng DOUBLE PRECISION,
    end_lat DOUBLE PRECISION,
    end_lng DOUBLE PRECISION,
    distance DOUBLE PRECISION NOT NULL,
    estimated_time DOUBLE PRECISION NOT NULL,
    child_friendliness INTEGER NOT NULL DEFAULT 0,
    owner_user_id BIGINT REFERENCES app_user(id) ON DELETE CASCADE,
    creator_name VARCHAR(255),
    route_image_path VARCHAR(255) NOT NULL,
    route_geometry TEXT,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS user_session (
    token VARCHAR(255) PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS tour_log (
    logid VARCHAR(255) PRIMARY KEY,
    date VARCHAR(255) NOT NULL,
    time VARCHAR(255) NOT NULL,
    comment TEXT NOT NULL,
    difficulty INTEGER NOT NULL,
    total_distance DOUBLE PRECISION NOT NULL,
    total_time DOUBLE PRECISION NOT NULL,
    rating INTEGER NOT NULL,
    tourid VARCHAR(255) NOT NULL REFERENCES tour(id) ON DELETE CASCADE,
    owner_user_id BIGINT REFERENCES app_user(id) ON DELETE CASCADE,
    creator_name VARCHAR(255) NOT NULL
);
