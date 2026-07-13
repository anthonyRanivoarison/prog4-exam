CREATE TABLE image_submission
(
    id         UUID         NOT NULL,
    filename   VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL,
    created_at TIMESTAMP    NOT NULL,
    CONSTRAINT pk_image_submission PRIMARY KEY (id)
);