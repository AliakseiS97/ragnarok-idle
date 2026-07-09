-- Система «Сага» (бывш. «редкость», GDD §3.8): у каждого героя ранг (1..N) и под-ступень (I..X).
-- Ранг задаёт потолок уровня («стену») и множитель DPS. Свежекупленный герой — ранг 1 «Обычный», ступень I.
ALTER TABLE player_hero ADD COLUMN saga_rank     INT NOT NULL DEFAULT 1;
ALTER TABLE player_hero ADD COLUMN saga_sub_step INT NOT NULL DEFAULT 1;
