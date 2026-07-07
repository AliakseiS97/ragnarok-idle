-- Балансировка Спринта 1 (плейтест): найм подешевел, DPS героев — растущая кривая.
--
-- Было: price = 50 × 12^(id-1) (2-й герой 600, 3-й 7200 — недостижимо на старте),
--       base_dps = 2 у всех (нет смысла покупать поздних героев).
-- Стало (ориентир — референсные кликеры, отражено в economy_constants.md):
--   price    = 50 × 5^(id-1)   -> 50, 250, 1250, ...
--   base_dps = 5 × 3^(id-1)    -> 5, 15, 45, ... (позже открылся — сильнее)

UPDATE hero SET price = '5|1',              base_dps = '5.0|0'       WHERE id = 1;
UPDATE hero SET price = '2.5|2',            base_dps = '1.5|1'       WHERE id = 2;
UPDATE hero SET price = '1.25|3',           base_dps = '4.5|1'       WHERE id = 3;
UPDATE hero SET price = '6.25|3',           base_dps = '1.35|2'      WHERE id = 4;
UPDATE hero SET price = '3.125|4',          base_dps = '4.05|2'      WHERE id = 5;
UPDATE hero SET price = '1.5625|5',         base_dps = '1.215|3'     WHERE id = 6;
UPDATE hero SET price = '7.8125|5',         base_dps = '3.645|3'     WHERE id = 7;
UPDATE hero SET price = '3.90625|6',        base_dps = '1.0935|4'    WHERE id = 8;
UPDATE hero SET price = '1.953125|7',       base_dps = '3.2805|4'    WHERE id = 9;
UPDATE hero SET price = '9.765625|7',       base_dps = '9.8415|4'    WHERE id = 10;
UPDATE hero SET price = '4.8828125|8',      base_dps = '2.95245|5'   WHERE id = 11;
UPDATE hero SET price = '2.44140625|9',     base_dps = '8.85735|5'   WHERE id = 12;
UPDATE hero SET price = '1.220703125|10',   base_dps = '2.657205|6'  WHERE id = 13;
UPDATE hero SET price = '6.103515625|10',   base_dps = '7.971615|6'  WHERE id = 14;
UPDATE hero SET price = '3.0517578125|11',  base_dps = '2.3914845|7' WHERE id = 15;
