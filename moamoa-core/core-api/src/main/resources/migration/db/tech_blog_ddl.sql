CREATE TABLE tech_blog
(
    id               BIGINT AUTO_INCREMENT NOT NULL,
    created_at       datetime              NOT NULL,
    last_modified_at datetime              NOT NULL,
    title            VARCHAR(255)          NOT NULL,
    tech_blog_key    VARCHAR(255)          NOT NULL,
    blog_url         VARCHAR(255)          NOT NULL,
    icon             VARCHAR(255)          NOT NULL,
    CONSTRAINT pk_tech_blog PRIMARY KEY (id)
);

ALTER TABLE tech_blog
    ADD CONSTRAINT uc_tech_blog_tech_blog_key UNIQUE (tech_blog_key);

ALTER TABLE tech_blog
    ADD CONSTRAINT uc_tech_blog_title UNIQUE (title);