-- Счётчик перерождений: дроп Пепла с мобов открывается только после 1-го перерождения (rebirthCount >= 1).
ALTER TABLE player ADD COLUMN rebirth_count BIGINT NOT NULL DEFAULT 0;
