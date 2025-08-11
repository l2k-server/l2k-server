
CREATE TABLE IF NOT EXISTS accounts (
    id SERIAL PRIMARY KEY,
    login VARCHAR(16) UNIQUE NOT NULL,
    password VARCHAR(32) NOT NULL,
    email VARCHAR(64) UNIQUE,
    creation_time TIMESTAMP NOT NULL,
    last_active TIMESTAMP NOT NULL,
    access_level SMALLINT NOT NULL,
    last_server SMALLINT,
    last_ip VARCHAR(15)
);
