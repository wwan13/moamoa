CREATE TABLE post_category
(
    id               BIGINT AUTO_INCREMENT NOT NULL,
    created_at       datetime              NOT NULL,
    last_modified_at datetime              NOT NULL,
    post_id          BIGINT                NOT NULL,
    category_id      BIGINT                NOT NULL,
    CONSTRAINT pk_post_category PRIMARY KEY (id)
);

ALTER TABLE post_category
    ADD CONSTRAINT FK_POST_CATEGORY_ON_CATEGORY FOREIGN KEY (category_id) REFERENCES category (id);

ALTER TABLE post_category
    ADD CONSTRAINT FK_POST_CATEGORY_ON_POST FOREIGN KEY (post_id) REFERENCES post (id);