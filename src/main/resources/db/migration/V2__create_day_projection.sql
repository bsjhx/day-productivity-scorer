CREATE TABLE day_projection
(
    id        UUID    NOT NULL PRIMARY KEY,
    user_id   UUID    NOT NULL,
    date      DATE    NOT NULL,
    score     INT     NOT NULL,
    is_locked BOOLEAN NOT NULL DEFAULT FALSE
);
