-- Урон/тап Аватара = уровень тапа (GDD §3.4): уровень стартует с 1 (урон 1), не с 0.
ALTER TABLE avatar ALTER COLUMN tap_damage_level SET DEFAULT 1;
UPDATE avatar SET tap_damage_level = 1 WHERE tap_damage_level < 1;
