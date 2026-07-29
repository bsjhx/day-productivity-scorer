CREATE TABLE users (
    id       UUID         NOT NULL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    roles    VARCHAR(255) NOT NULL
);
