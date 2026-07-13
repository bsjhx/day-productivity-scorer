CREATE TABLE day_projection
(
    id        DATE    NOT NULL PRIMARY KEY,
    score     INT     NOT NULL,
    is_locked BOOLEAN NOT NULL DEFAULT FALSE
);
