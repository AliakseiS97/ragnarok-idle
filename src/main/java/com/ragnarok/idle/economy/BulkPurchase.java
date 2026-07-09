package com.ragnarok.idle.economy;

import com.ragnarok.idle.math.BigNum;
import java.util.function.LongFunction;

/**
 * Расчёт стоимости пачки уровней при мультипокупке (общий для героев и Аватара).
 * <p>
 * Ключевое: суммарная цена — это сумма растущей прогрессии цен уровней
 * ({@code Σ costOf(level+i)}), а НЕ {@code цена × N}. Ничего не списывает и не меняет —
 * только считает, сколько уровней и за сколько золота. Реальное списание — в сервисах под {@code @Transactional}.
 */
public final class BulkPurchase {

    /** Предохранитель от рантаунного цикла в MAX при аномально большом золоте (у Аватара нет потолка уровня). */
    private static final long MAX_BULK_LEVELS = 1_000_000L;

    private BulkPurchase() {
    }

    /** Итог расчёта пачки: сколько уровней и полная суммарная цена. */
    public record Quote(long levels, BigNum totalCost) {
    }

    /**
     * @param costOf     цена перехода с уровня L на L+1 (растущая формула конкретной сущности)
     * @param startLevel текущий уровень
     * @param mode       режим мультипокупки
     * @param gold       доступное золото — используется ТОЛЬКО для {@link PurchaseMode#MAX}
     * @param maxLevel   жёсткий потолок уровня (стена Саги у героев; {@link Long#MAX_VALUE} — без потолка)
     * @return для фикс. режима — ровно N уровней и их суммарная цена (без оглядки на золото и потолок —
     *         доступность проверяет вызывающий); для MAX — сколько влезло в золото и не пробило потолок.
     */
    public static Quote quote(LongFunction<BigNum> costOf, long startLevel, PurchaseMode mode,
                              BigNum gold, long maxLevel) {
        if (mode.isMax()) {
            return maxQuote(costOf, startLevel, gold, maxLevel);
        }
        long n = mode.count();
        BigNum total = BigNum.ZERO;
        for (long i = 0; i < n; i++) {
            total = total.add(costOf.apply(startLevel + i));
        }
        return new Quote(n, total);
    }

    private static Quote maxQuote(LongFunction<BigNum> costOf, long startLevel, BigNum gold, long maxLevel) {
        BigNum total = BigNum.ZERO;
        long bought = 0;
        long level = startLevel;
        while (level < maxLevel && bought < MAX_BULK_LEVELS) {
            BigNum afterNext = total.add(costOf.apply(level));
            if (gold.lt(afterNext)) {
                break; // на следующий уровень уже не хватает — покупаем только то, что влезло
            }
            total = afterNext;
            bought++;
            level++;
        }
        return new Quote(bought, total);
    }
}
