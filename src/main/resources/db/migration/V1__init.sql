-- Спринт 0: минимальная схема. Расширяется в следующих спринтах.
-- Игрок. Денежные величины хранятся как строки big-number "mantissa|exponent".
CREATE TABLE player (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username            VARCHAR(50)  NOT NULL UNIQUE,
    password_hash       VARCHAR(255) NOT NULL,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    current_level       BIGINT       NOT NULL DEFAULT 1,
    max_level           BIGINT       NOT NULL DEFAULT 1,
    gold                VARCHAR(64)  NOT NULL DEFAULT '0|0',
    ash                 VARCHAR(64)  NOT NULL DEFAULT '0|0',
    last_collected_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    delete_pending_until TIMESTAMP   NULL
);
