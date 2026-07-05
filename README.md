# Рагнарёк: Восхождение — бэкенд

Idle/AFK кликер на скандинавской мифологии. Spring Boot + PostgreSQL.
Полный дизайн и формулы — в `docs/MASTER_GDD.md`, числа — в `docs/economy_constants.md`.

## Запуск (Спринт 0)
Нужен JDK 17+ и Maven.

```bash
mvn spring-boot:run
```

По умолчанию поднимается на H2 в памяти (без установки Postgres):
- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 консоль: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:ragnarok`)

## Тесты
```bash
mvn test
```
Ключевой тест — `BigNumTest`: проверяет игровую математику на числах, которые
переполнили бы обычный double (уровни 5000+).

## Что готово (Спринт 0)
- Каркас Spring Boot, Maven, профиль H2/Postgres.
- **Класс `BigNum`** (мантисса+экспонента) — сердце игровой математики, с тестами.
- Flyway-миграция V1 (таблица player).
- Swagger подключён.

## Дальше (по дорожной карте GDD, часть XIII)
Спринт 1 — MVP-кликер: Player/Avatar, золото, AFK-доход, кликер, 15 стартовых
героев, перерождение + Пепел.
