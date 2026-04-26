CREATE TABLE post_tag
(
    id               BIGINT AUTO_INCREMENT NOT NULL,
    created_at       datetime              NOT NULL,
    last_modified_at datetime              NOT NULL,
    post_id          BIGINT                NOT NULL,
    tag_id      BIGINT                NOT NULL,
    CONSTRAINT pk_post_tag PRIMARY KEY (id)
);

ALTER TABLE post_tag
    ADD CONSTRAINT FK_POST_TAG_ON_TAG FOREIGN KEY (tag_id) REFERENCES tag (id);

ALTER TABLE post_tag
    ADD CONSTRAINT FK_POST_TAG_ON_POST FOREIGN KEY (post_id) REFERENCES post (id);