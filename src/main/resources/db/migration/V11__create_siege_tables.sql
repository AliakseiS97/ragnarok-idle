-- Осадные боссы (недельная ротация) и их защитники. Первый слайс: только данные + read-only API.
-- Активный босс недели выбирается по rotation_order = floorMod(weeksSinceLaunch, 7) + 1 (см. SiegeScheduleService).

CREATE TABLE siege_bosses (
    id BIGSERIAL PRIMARY KEY,
    boss_key VARCHAR(50) NOT NULL UNIQUE,
    name_ru VARCHAR(50) NOT NULL,
    name_original VARCHAR(50) NOT NULL,
    rotation_order INT NOT NULL UNIQUE,             -- 1..7, порядок понедельной ротации
    base_hp NUMERIC(19,0) NOT NULL,
    spawn_chance NUMERIC(5,2) NOT NULL,             -- 25 / 15 / 5 (под будущий спавн-ролл)
    hp_multiplier NUMERIC(4,2) NOT NULL,            -- 1 / 2 / 4 (шанс 25%->x1, 15%->x2, 5%->x4)
    enrage_seconds INT,                             -- таймер до вайпа; NULL у боссов без него
    skill1_name VARCHAR(100) NOT NULL,
    skill1_description VARCHAR(255) NOT NULL,
    skill2_name VARCHAR(100) NOT NULL,
    skill2_description VARCHAR(255) NOT NULL,
    -- умение босса как героя после найма — понадобится в следующей итерации
    hire_skill_name VARCHAR(100) NOT NULL,
    hire_skill_description VARCHAR(255) NOT NULL,
    min_recommended_tier INT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE siege_defenders (
    id BIGSERIAL PRIMARY KEY,
    boss_id BIGINT NOT NULL REFERENCES siege_bosses(id),
    name_ru VARCHAR(50) NOT NULL,
    position INT NOT NULL,                          -- порядок в бою
    hp_percent_of_boss NUMERIC(5,2) NOT NULL,
    skill_description VARCHAR(255) NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);
