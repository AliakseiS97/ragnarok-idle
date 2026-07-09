package com.ragnarok.idle.domain;

/**
 * Ранг Саги героя — «рамка», задающая потолок уровня («стену») и визуальный маркер.
 * Система «Сага» (бывш. «редкость», GDD §3.8): у каждого героя есть ранг (1..N) и под-ступень
 * (I..X внутри ранга, см. {@link Saga}).
 *
 * <p><b>Потолок ранга = стена:</b> герой не качается за {@link #getLevelCap()} своего ранга, пока
 * не поднимет Сагу (см. проверку в {@code HeroService.upgrade}).
 *
 * <p><b>Расширяемость:</b> код НЕ закладывает «ровно 7 рангов» — {@link #maxRank()} и {@link #next()}
 * выводятся из числа констант. После 7-го ранга планируются ещё 3 уровня под слоты экипировки
 * (сапоги/штаны/нагрудник/перчатки) — их достаточно дописать сюда новыми константами (Этап 3+),
 * менять логику потолка/множителя/промоушена не придётся.
 *
 * <p>Названия, потолки и цвета-маркеры — здесь, чтобы баланс/визуал правились в одном месте.
 * Цвет — временный индикатор-заглушка под будущие рамки.
 */
public enum SagaRank {

    COMMON("Обычный", 500, "#9aa4b0"),
    RARE("Редкий", 1_000, "#4caf50"),
    LEGENDARY("Легендарный", 2_500, "#2196f3"),
    MYTHIC("Мифический", 5_000, "#9c27b0"),
    DIVINE("Божественный", 10_000, "#ff9800"),
    ASGARDIAN("Асгардский", 20_000, "#e53935"),
    PRIMORDIAL("Изначальный", 30_000, "#00e5ff");

    private final String displayName;
    private final long levelCap;
    private final String color;

    SagaRank(String displayName, long levelCap, String color) {
        this.displayName = displayName;
        this.levelCap = levelCap;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Максимальный уровень героя в этом ранге — «стена», которую пробивают повышением Саги. */
    public long getLevelCap() {
        return levelCap;
    }

    /** Цвет-маркер ранга для UI (заглушка под будущие рамки). */
    public String getColor() {
        return color;
    }

    /** Номер ранга 1..N (в БД хранится именно он, а не ordinal). */
    public int rank() {
        return ordinal() + 1;
    }

    /** true, если это последний доступный ранг (дальше повышать некуда — пока). */
    public boolean isMax() {
        return ordinal() == values().length - 1;
    }

    /** Следующий ранг; бросает, если уже максимальный (вызывающий обязан проверить {@link #isMax()}). */
    public SagaRank next() {
        if (isMax()) {
            throw new IllegalStateException("Нет ранга выше " + displayName);
        }
        return values()[ordinal() + 1];
    }

    /** Ранг по номеру 1..N. */
    public static SagaRank byRank(int rank) {
        SagaRank[] all = values();
        if (rank < 1 || rank > all.length) {
            throw new IllegalArgumentException("Ранг Саги вне диапазона 1.." + all.length + ": " + rank);
        }
        return all[rank - 1];
    }

    /** Число рангов — единый источник «сколько всего ступеней рангов», без хардкода. */
    public static int maxRank() {
        return values().length;
    }
}
