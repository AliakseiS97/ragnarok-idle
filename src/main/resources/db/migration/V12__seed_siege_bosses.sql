-- Сид осадных боссов (7) и их защитников (38).
-- HP статично от редкости: currentHp = base_hp * hp_multiplier (шанс 25%->x1, 15%->x2, 5%->x4).
-- База по тирам: тир1 ~1e9, тир2 ~1e11, тир3 ~1e13. Проценты защитников — плейсхолдеры первого слайса.

INSERT INTO siege_bosses
    (boss_key, name_ru, name_original, rotation_order, base_hp, spawn_chance, hp_multiplier, enrage_seconds,
     skill1_name, skill1_description, skill2_name, skill2_description,
     hire_skill_name, hire_skill_description, min_recommended_tier) VALUES
('siege_fenrir', 'Фенрир', 'Fenrir', 1, 1000000000, 25.00, 1.00, NULL,
 'Пожирание', 'Выводит героя из боя на 15 сек',
 'Разрыв Глейпнира', '+100% урона ниже 50% HP',
 'Укус', 'Игнорирует 50% защиты боссов', 1),
('siege_jormungandr', 'Ёрмунганд', 'Jörmungandr', 2, 1000000000, 25.00, 1.00, NULL,
 'Погружение', 'Неуязвим 10 сек',
 'Яд', '-5% DPS команды в стак',
 'Ядовитая аура', 'DoT всем врагам', 1),
('siege_hel', 'Хель', 'Hel', 3, 1000000000, 25.00, 1.00, NULL,
 'Воскрешение', 'Поднимает павшего защитника',
 'Мир мёртвых', 'Половина урона по ней возвращается позже',
 'Половина смерти', 'Переживает смертельный удар с 50% HP', 1),
('siege_surtr', 'Сурт', 'Surtr', 4, 100000000000, 15.00, 2.00, 180,
 'Пламенный меч', 'Сжигает бафы команды',
 'Пожар Муспельхейма', 'Урон всем героям',
 'Испепеление', 'Сжигает 3% HP босса мгновенно', 2),
('siege_nidhoggr', 'Нидхёгг', 'Níðhöggr', 5, 100000000000, 15.00, 2.00, NULL,
 'Грыз корня', 'Каждые 30 сек -10% макс. HP команды',
 'Взлёт', 'Уязвим только для дальнего боя',
 'Пожиратель корней', '+30% урона по живым боссам', 2),
('siege_ymir', 'Имир', 'Ymir', 6, 10000000000000, 5.00, 4.00, NULL,
 'Ледяная броня', 'Ломается только кликами',
 'Кровь турсов', 'Из ран рождаются новые защитники',
 'Плоть мира', '+100% HP всей команде', 3),
('siege_garmr', 'Гарм', 'Garmr', 7, 10000000000000, 5.00, 4.00, NULL,
 'Вой', 'Молчание умений 8 сек',
 'Свирепость', 'Усиливается с каждой смертью героя',
 'Цепной пёс', 'Контратакует умения боссов', 3);

-- Защитники (FK по бизнес-ключу boss_key). Позиции задают порядок в бою.
INSERT INTO siege_defenders (boss_id, name_ru, position, hp_percent_of_boss, skill_description) VALUES
-- Фенрир (5)
((SELECT id FROM siege_bosses WHERE boss_key='siege_fenrir'), 'Скёлль', 1, 30.00, 'Волк, преследующий солнце — ускоряется со временем боя'),
((SELECT id FROM siege_bosses WHERE boss_key='siege_fenrir'), 'Хати', 2, 30.00, 'Волк, преследующий луну — бьёт по самому слабому герою'),
((SELECT id FROM siege_bosses WHERE boss_key='siege_fenrir'), 'Ульфхеднар 1', 3, 15.00, 'Воин-волк в исступлении, рвёт передовую линию'),
((SELECT id FROM siege_bosses WHERE boss_key='siege_fenrir'), 'Ульфхеднар 2', 4, 15.00, 'Воин-волк в исступлении, рвёт передовую линию'),
((SELECT id FROM siege_bosses WHERE boss_key='siege_fenrir'), 'Ульфхеднар 3', 5, 15.00, 'Воин-волк в исступлении, рвёт передовую линию'),
-- Ёрмунганд (9 дочерей Эгира)
((SELECT id FROM siege_bosses WHERE boss_key='siege_jormungandr'), 'Химинглава', 1, 12.00, 'Волна прозрачности — рассеивает щиты'),
((SELECT id FROM siege_bosses WHERE boss_key='siege_jormungandr'), 'Дува', 2, 12.00, 'Тихая волна — скрывает союзников от прицела'),
((SELECT id FROM siege_bosses WHERE boss_key='siege_jormungandr'), 'Блодухадда', 3, 12.00, 'Кровавая волна — усиливает урон по раненым'),
((SELECT id FROM siege_bosses WHERE boss_key='siege_jormungandr'), 'Хевринг', 4, 12.00, 'Вздымающаяся волна — отбрасывает героев'),
((SELECT id FROM siege_bosses WHERE boss_key='siege_jormungandr'), 'Унн', 5, 12.00, 'Пенная волна — накрывает область ядом'),
((SELECT id FROM siege_bosses WHERE boss_key='siege_jormungandr'), 'Хрённ', 6, 12.00, 'Хватающая волна — стягивает героя к боссу'),
((SELECT id FROM siege_bosses WHERE boss_key='siege_jormungandr'), 'Бюльгья', 7, 12.00, 'Вздутая волна — глушит умения на миг'),
((SELECT id FROM siege_bosses WHERE boss_key='siege_jormungandr'), 'Дрёвн', 8, 12.00, 'Гребень волны — режущий удар по линии'),
((SELECT id FROM siege_bosses WHERE boss_key='siege_jormungandr'), 'Кольга', 9, 12.00, 'Холодная волна — замедляет команду'),
-- Хель (7)
((SELECT id FROM siege_bosses WHERE boss_key='siege_hel'), 'Модгуд', 1, 25.00, 'Страж моста Гьялларбру — не пропускает урон мимо себя'),
((SELECT id FROM siege_bosses WHERE boss_key='siege_hel'), 'Драуг 1', 2, 12.00, 'Оживший мертвец, тянет героев в землю'),
((SELECT id FROM siege_bosses WHERE boss_key='siege_hel'), 'Драуг 2', 3, 12.00, 'Оживший мертвец, тянет героев в землю'),
((SELECT id FROM siege_bosses WHERE boss_key='siege_hel'), 'Драуг 3', 4, 12.00, 'Оживший мертвец, тянет героев в землю'),
((SELECT id FROM siege_bosses WHERE boss_key='siege_hel'), 'Драуг 4', 5, 12.00, 'Оживший мертвец, тянет героев в землю'),
((SELECT id FROM siege_bosses WHERE boss_key='siege_hel'), 'Драуг 5', 6, 12.00, 'Оживший мертвец, тянет героев в землю'),
((SELECT id FROM siege_bosses WHERE boss_key='siege_hel'), 'Щенок Гарма', 7, 20.00, 'Молодой адский пёс — воет, снижая точность'),
-- Сурт (4)
((SELECT id FROM siege_bosses WHERE boss_key='siege_surtr'), 'Огненный великан I', 1, 25.00, 'Муспельский страж — поджигает при касании'),
((SELECT id FROM siege_bosses WHERE boss_key='siege_surtr'), 'Огненный великан II', 2, 25.00, 'Муспельский страж — поджигает при касании'),
((SELECT id FROM siege_bosses WHERE boss_key='siege_surtr'), 'Огненный великан III', 3, 25.00, 'Муспельский страж — поджигает при касании'),
((SELECT id FROM siege_bosses WHERE boss_key='siege_surtr'), 'Огненный великан IV', 4, 25.00, 'Муспельский страж — поджигает при касании'),
-- Нидхёгг (4)
((SELECT id FROM siege_bosses WHERE boss_key='siege_nidhoggr'), 'Змей корней 1', 1, 20.00, 'Грызёт корни Иггдрасиля — снижает макс. HP команды'),
((SELECT id FROM siege_bosses WHERE boss_key='siege_nidhoggr'), 'Змей корней 2', 2, 20.00, 'Грызёт корни Иггдрасиля — снижает макс. HP команды'),
((SELECT id FROM siege_bosses WHERE boss_key='siege_nidhoggr'), 'Змей корней 3', 3, 20.00, 'Грызёт корни Иггдрасиля — снижает макс. HP команды'),
((SELECT id FROM siege_bosses WHERE boss_key='siege_nidhoggr'), 'Тень Хрёсвельга', 4, 30.00, 'Орёл на краю неба — поднимает бурю, мешает ближнему бою'),
-- Имир (6)
((SELECT id FROM siege_bosses WHERE boss_key='siege_ymir'), 'Хримтурс I', 1, 15.00, 'Инеистый турс — покрывает героев льдом'),
((SELECT id FROM siege_bosses WHERE boss_key='siege_ymir'), 'Хримтурс II', 2, 15.00, 'Инеистый турс — покрывает героев льдом'),
((SELECT id FROM siege_bosses WHERE boss_key='siege_ymir'), 'Хримтурс III', 3, 15.00, 'Инеистый турс — покрывает героев льдом'),
((SELECT id FROM siege_bosses WHERE boss_key='siege_ymir'), 'Хримтурс IV', 4, 15.00, 'Инеистый турс — покрывает героев льдом'),
((SELECT id FROM siege_bosses WHERE boss_key='siege_ymir'), 'Хримтурс V', 5, 15.00, 'Инеистый турс — покрывает героев льдом'),
((SELECT id FROM siege_bosses WHERE boss_key='siege_ymir'), 'Хримтурс VI', 6, 15.00, 'Инеистый турс — покрывает героев льдом'),
-- Гарм (3)
((SELECT id FROM siege_bosses WHERE boss_key='siege_garmr'), 'Страж Гнипахеллира I', 1, 30.00, 'Цепной пёс у входа в Хель — контратакует умения'),
((SELECT id FROM siege_bosses WHERE boss_key='siege_garmr'), 'Страж Гнипахеллира II', 2, 30.00, 'Цепной пёс у входа в Хель — контратакует умения'),
((SELECT id FROM siege_bosses WHERE boss_key='siege_garmr'), 'Страж Гнипахеллира III', 3, 30.00, 'Цепной пёс у входа в Хель — контратакует умения');
