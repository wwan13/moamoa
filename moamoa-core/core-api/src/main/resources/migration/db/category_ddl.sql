CREATE TABLE category
(
    id               BIGINT AUTO_INCREMENT NOT NULL,
    created_at       datetime              NOT NULL,
    last_modified_at datetime              NOT NULL,
    title            VARCHAR(255)          NOT NULL,
    CONSTRAINT pk_category PRIMARY KEY (id)
);

ALTER TABLE category
    ADD CONSTRAINT uc_category_title UNIQUE (title);

CREATE UNIQUE INDEX idx_category_title ON category (title);