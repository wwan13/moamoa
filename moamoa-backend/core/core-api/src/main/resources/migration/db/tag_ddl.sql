CREATE TABLE tag
(
    id               BIGINT AUTO_INCREMENT NOT NULL,
    created_at       datetime              NOT NULL,
    last_modified_at datetime              NOT NULL,
    title            VARCHAR(255)          NOT NULL,
    CONSTRAINT pk_tag PRIMARY KEY (id)
);

ALTER TABLE tag
    ADD CONSTRAINT uc_tag_title UNIQUE (title);

CREATE UNIQUE INDEX idx_tag_title ON tag (title);