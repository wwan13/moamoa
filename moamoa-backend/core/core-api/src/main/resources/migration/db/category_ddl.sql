CREATE TABLE category
(
    id    BIGINT      NOT NULL,
    `key` VARCHAR(50) NOT NULL,
    title VARCHAR(50) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_category_key (`key`)
);