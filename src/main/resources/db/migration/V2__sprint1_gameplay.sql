-- Спринт 1: игровой цикл — кликер, стартовые герои, состояние боя игрока.

ALTER TABLE player ADD COLUMN current_sub_level INT NOT NULL DEFAULT 1;
ALTER TABLE player ADD COLUMN current_mob_hp VARCHAR(64) NOT NULL DEFAULT '1.0|1';

CREATE TABLE avatar (
    player_id        BIGINT PRIMARY KEY REFERENCES player(id),
    tap_damage_level BIGINT NOT NULL DEFAULT 0,
    autotap_level    BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE hero (
    id             BIGINT PRIMARY KEY,
    name           VARCHAR(100) NOT NULL,
    type           VARCHAR(20)  NOT NULL,
    base_dps       VARCHAR(64)  NOT NULL,
    skill_code     VARCHAR(50)  NOT NULL,
    price          VARCHAR(64)  NOT NULL,
    special_baffer BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE TABLE player_hero (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    player_id BIGINT  NOT NULL REFERENCES player(id),
    hero_id   BIGINT  NOT NULL REFERENCES hero(id),
    level     BIGINT  NOT NULL DEFAULT 1,
    activated BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (player_id, hero_id)
);

-- 15 стартовых героев (economy_constants.md, лист "Все герои (seed)").
-- base_dps единый для всех (2) — индивидуальных значений в источнике нет (см. план Спринта 1).
-- price = 50 * 12^(id-1) ("Цена 1-го героя"=50, "Множ. цены найма"=12), нормализовано в mantissa|exponent.
INSERT INTO hero (id, name, type, base_dps, skill_code, price, special_baffer) VALUES
(1,  'Трэлл',                'ACTIVE',  '2.0|0', 'AUTOTAP_BASIC',         '5|1',                 FALSE),
(2,  'Рыбак с Лофотен',      'ACTIVE',  '2.0|0', 'GOLD_ON_TAP',           '6|2',                 FALSE),
(3,  'Скальд',               'BAFFER',  '2.0|0', 'BAFFER_DPS_PER_LEVEL',  '7.2|3',               FALSE),
(4,  'Охотница фьордов',     'PASSIVE', '2.0|0', 'CRIT_CHANCE_PER_LEVEL', '8.64|4',              FALSE),
(5,  'Щитоносица',           'PASSIVE', '2.0|0', 'DPS_BONUS_PER_LEVEL',   '1.0368|6',            FALSE),
(6,  'Берсерк',              'ACTIVE',  '2.0|0', 'DOUBLE_DAMAGE_10S',     '1.24416|7',           FALSE),
(7,  'Вёльва',               'ACTIVE',  '2.0|0', 'COOLDOWN_REDUCTION',    '1.492992|8',          FALSE),
(8,  'Гном Брокк',           'PASSIVE', '2.0|0', 'UPGRADE_DISCOUNT',      '1.7915904|9',         FALSE),
(9,  'Ульфхеднар',           'ACTIVE',  '2.0|0', 'EVERY_10TH_TAP_X50',    '2.14990848|10',       FALSE),
(10, 'Капитан драккара',     'PASSIVE', '2.0|0', 'BOSS_GOLD_BONUS',       '2.579890176|11',      FALSE),
(11, 'Эйнхерий',             'BAFFER',  '2.0|0', 'BAFFER_AMPLIFIER_X3',   '3.0958682112|12',     TRUE),
(12, 'Валькирия-новобранец', 'PASSIVE', '2.0|0', 'DPS_PER_HERO_OWNED',    '3.71504185344|13',    FALSE),
(13, 'Хульдра',              'ACTIVE',  '2.0|0', 'SKILL_BUFF',            '4.458050224128|14',   FALSE),
(14, 'Драуг',                'PASSIVE', '2.0|0', 'DPS_PER_KILL_STACK',    '5.3496602689536|15',  FALSE),
(15, 'Ледяной ётун',         'REBIRTH', '2.0|0', 'REBIRTH_UNLOCK',        '6.41959232274432|16', FALSE);
