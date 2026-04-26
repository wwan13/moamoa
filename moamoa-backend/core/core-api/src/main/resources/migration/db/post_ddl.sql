CREATE TABLE post
(
    id               BIGINT AUTO_INCREMENT NOT NULL,
    created_at       datetime              NOT NULL,
    last_modified_at datetime              NOT NULL,
    post_key         VARCHAR(255)          NOT NULL,
    title            VARCHAR(255)          NOT NULL,
    `description`    VARCHAR(255)          NOT NULL,
    thumbnail        VARCHAR(255)          NOT NULL,
    url              VARCHAR(255)          NOT NULL,
    tech_blog_id     BIGINT                NOT NULL,
    CONSTRAINT pk_post PRIMARY KEY (id)
);

ALTER TABLE post
    ADD CONSTRAINT FK_POST_ON_TECH_BLOG FOREIGN KEY (tech_blog_id) REFERENCES tech_blog (id);

ALTER TABLE post
    ADD CONSTRAINT uq_post_tech_blog_key
        UNIQUE (tech_blog_id, post_key);