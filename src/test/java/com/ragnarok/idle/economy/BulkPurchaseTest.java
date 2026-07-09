package com.ragnarok.idle.economy;

import com.ragnarok.idle.math.BigNum;
import java.util.function.LongFunction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Ядро мультипокупки: сумма растущей прогрессии, а не цена×N, и границы MAX (GDD §12.7). */
class BulkPurchaseTest {

    // Растущая цена: уровень L → L+1 стоит 100×L (100, 200, 300, ...). Явно возрастает, чтобы
    // сумма прогрессии отличалась от цена×N и ловила баг «умножили первую цену на N».
    private static final LongFunction<BigNum> COST = level -> BigNum.of(100L * level);
    private static final long NO_CAP = Long.MAX_VALUE;

    @Test
    void fixedModeSumsProgressionNotFirstPriceTimesN() {
        // x5 с ур.1: 100+200+300+400+500 = 1500 (цена×N дала бы 100×5 = 500).
        BulkPurchase.Quote quote = BulkPurchase.quote(COST, 1, PurchaseMode.X5, BigNum.ZERO, NO_CAP);

        assertEquals(5, quote.levels());
        assertEquals(BigNum.of(1500), quote.totalCost(), "сумма прогрессии, а не 100×5=500");
    }

    @Test
    void fixedModeStartsFromCurrentLevel() {
        // x5 с ур.3: 300+400+500+600+700 = 2500 — прогрессия считается от текущего уровня.
        BulkPurchase.Quote quote = BulkPurchase.quote(COST, 3, PurchaseMode.X5, BigNum.ZERO, NO_CAP);

        assertEquals(5, quote.levels());
        assertEquals(BigNum.of(2500), quote.totalCost());
    }

    @Test
    void maxBuysAsManyLevelsAsGoldAllows() {
        // золото 1500 → ровно 5 уровней (100+200+300+400+500); 6-й (600) уже не влезает.
        BulkPurchase.Quote quote = BulkPurchase.quote(COST, 1, PurchaseMode.MAX, BigNum.of(1500), NO_CAP);

        assertEquals(5, quote.levels());
        assertEquals(BigNum.of(1500), quote.totalCost());
    }

    @Test
    void maxWithGoldOneShortDropsTheLastLevel() {
        // золото 1499 → 4 уровня (сумма 1000): на 5-й нужно ещё 500, а есть лишь 499.
        BulkPurchase.Quote quote = BulkPurchase.quote(COST, 1, PurchaseMode.MAX, BigNum.of(1499), NO_CAP);

        assertEquals(4, quote.levels());
        assertEquals(BigNum.of(1000), quote.totalCost());
    }

    @Test
    void maxBuysZeroWhenGoldBelowFirstLevel() {
        // золото 99 < цены 1-го уровня (100) → 0 уровней, 0 цена (граница «на 0 уровней»).
        BulkPurchase.Quote quote = BulkPurchase.quote(COST, 1, PurchaseMode.MAX, BigNum.of(99), NO_CAP);

        assertEquals(0, quote.levels());
        assertEquals(BigNum.ZERO, quote.totalCost());
    }

    @Test
    void maxStopsAtLevelCap() {
        // денег вагон, но потолок на ур.4 → купить можно только 3 уровня (ур.1→4).
        BulkPurchase.Quote quote = BulkPurchase.quote(COST, 1, PurchaseMode.MAX, BigNum.of(1e12), 4);

        assertEquals(3, quote.levels());
        assertEquals(BigNum.of(600), quote.totalCost()); // 100+200+300
    }
}
