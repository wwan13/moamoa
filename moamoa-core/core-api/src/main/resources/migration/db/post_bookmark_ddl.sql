CREATE TABLE post_bookmark
(
    id               BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,

    member_id        BIGINT      NOT NULL,
    post_id          BIGINT      NOT NULL,

    created_at       DATETIME(6) NOT NULL,
    last_modified_at DATETIME(6) NOT NULL,

    UNIQUE KEY uk_member_post (member_id, post_id),
    KEY idx_member_id (member_id),
    KEY idx_post_id (post_id),

    CONSTRAINT fk_post_bookmark_member
        FOREIGN KEY (member_id)
            REFERENCES member (id),

    CONSTRAINT fk_post_bookmark_post
        FOREIGN KEY (post_id)
            REFERENCES post (id)
);