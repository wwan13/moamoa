CREATE TABLE submission
(
    id               BIGINT AUTO_INCREMENT NOT NULL,
    created_at       datetime              NOT NULL,
    last_modified_at datetime              NOT NULL,
    blog_title       VARCHAR(255)          NOT NULL,
    blog_url         VARCHAR(255)          NOT NULL,
    accepted         TINYINT(1)            NOT NULL,
    member_id        BIGINT                NOT NULL,
    CONSTRAINT pk_submission PRIMARY KEY (id)
);
