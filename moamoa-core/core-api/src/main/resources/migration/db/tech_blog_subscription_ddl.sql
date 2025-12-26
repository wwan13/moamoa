CREATE TABLE tech_blog_subscription
(
    id                   BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,

    member_id            BIGINT      NOT NULL,
    tech_blog_id         BIGINT      NOT NULL,

    notification_enabled TINYINT(1)  NOT NULL DEFAULT 1,

    created_at           DATETIME(6) NOT NULL,
    last_modified_at     DATETIME(6) NOT NULL,

    UNIQUE KEY uk_member_tech_blog (member_id, tech_blog_id),
    KEY idx_member_id (member_id),
    KEY idx_tech_blog_id (tech_blog_id),

    CONSTRAINT fk_tbs_member
        FOREIGN KEY (member_id)
            REFERENCES member (id),

    CONSTRAINT fk_tbs_tech_blog
        FOREIGN KEY (tech_blog_id)
            REFERENCES tech_blog (id)
);