-- Оптимистическая блокировка на Player/Avatar: без неё конкурентные read-modify-write
-- (тап / тик пассивного DPS / ребёрт) молча теряют чужие изменения ("последний записавший побеждает").
ALTER TABLE player ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE avatar ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
