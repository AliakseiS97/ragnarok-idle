package com.ragnarok.idle.economy;

/**
 * Режим мультипокупки улучшений (общий переключатель для героев и Аватара).
 * Клиент шлёт РЕЖИМ (действие), а не число уровней/цену — цену пачки считает сервер (античит, GDD §12.7).
 * <ul>
 *   <li>Фиксированные {@code X5..X100} — покупают РОВНО N уровней (или блокируются, если не хватает на все N);</li>
 *   <li>{@link #MAX} — покупает максимум уровней за текущее золото.</li>
 * </ul>
 */
public enum PurchaseMode {
    X1(1),
    X5(5),
    X10(10),
    X25(25),
    X50(50),
    X100(100),
    /** Столько уровней, сколько влезает в текущее золото (может быть 0). */
    MAX(-1);

    private final int count;

    PurchaseMode(int count) {
        this.count = count;
    }

    public boolean isMax() {
        return this == MAX;
    }

    /** Число уровней для фиксированного режима. Для {@link #MAX} не определено — сперва проверяй {@link #isMax()}. */
    public int count() {
        if (isMax()) {
            throw new IllegalStateException("MAX не имеет фиксированного count — резолвится по золоту");
        }
        return count;
    }
}
