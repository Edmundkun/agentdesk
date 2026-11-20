CREATE TABLE IF NOT EXISTS users
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    username   VARCHAR(64)  NOT NULL,
    password   VARCHAR(100) NOT NULL,
    nickname   VARCHAR(64)  NOT NULL,
    role       VARCHAR(32)  NOT NULL DEFAULT 'USER',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS documents
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    name        VARCHAR(255) NOT NULL,
    file_type   VARCHAR(16)  NOT NULL,
    size_bytes  BIGINT       NOT NULL DEFAULT 0,
    chunk_count INT          NOT NULL DEFAULT 0,
    status      VARCHAR(16)  NOT NULL DEFAULT 'READY',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_documents_user (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS document_chunks
(
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    document_id BIGINT   NOT NULL,
    user_id     BIGINT   NOT NULL,
    chunk_index INT      NOT NULL,
    content     TEXT     NOT NULL,
    embedding   TEXT     NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_chunks_document (document_id),
    KEY idx_chunks_user (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS conversations
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    title      VARCHAR(120) NOT NULL DEFAULT '新对话',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_conversations_user (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS messages
(
    id              BIGINT   NOT NULL AUTO_INCREMENT,
    conversation_id BIGINT   NOT NULL,
    role            VARCHAR(16) NOT NULL,
    content         MEDIUMTEXT NOT NULL,
    trace_json      MEDIUMTEXT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_messages_conversation (conversation_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
