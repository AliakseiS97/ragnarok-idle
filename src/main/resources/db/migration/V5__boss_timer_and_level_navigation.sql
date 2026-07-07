-- Таймер босса (30 сек) и навигация по уровням: флаг автоперехода + отметка начала боя с боссом.
ALTER TABLE player ADD COLUMN auto_advance BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE player ADD COLUMN boss_started_at TIMESTAMP;

-- старые данные: в прежней схеме босс был 11-й подлокацией — возвращаем таких игроков в начало уровня
UPDATE player SET current_sub_level = 1 WHERE current_sub_level > 10;
