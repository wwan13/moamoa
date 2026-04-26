CREATE TABLE member
(
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    role             VARCHAR(50)  NOT NULL,
    email            VARCHAR(255) NOT NULL,
    pw               VARCHAR(255) NOT NULL,
    created_at       datetime     NOT NULL,
    last_modified_at datetime     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_member_email (email)
) ENGINE = InnoDB;