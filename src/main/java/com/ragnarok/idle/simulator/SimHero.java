package com.ragnarok.idle.simulator;

import com.ragnarok.idle.domain.HeroType;
import com.ragnarok.idle.math.BigNum;
import java.util.List;

/**
 * Справочник 15 стартовых героев для симулятора. Значения price/baseDps — ТОЧНО те же строки
 * mantissa|exponent, что в сиде БД, чтобы симулятор считал по реальным числам.
 *
 * mirrors V2__sprint1_gameplay.sql (types, special_baffer) + V3__rebalance_hero_seed.sql (price, base_dps).
 * Если баланс сида поменяется — синхронизировать здесь.
 */
public record SimHero(int id, String name, HeroType type, boolean specialBaffer, BigNum price, BigNum baseDps) {

    private static SimHero of(int id, String name, HeroType type, boolean special, String price, String baseDps) {
        return new SimHero(id, name, type, special, BigNum.fromStorageString(price), BigNum.fromStorageString(baseDps));
    }

    /** 15 героев в порядке id (mirrors hero seed). */
    public static final List<SimHero> CATALOG = List.of(
            of(1,  "Трэлл",                HeroType.ACTIVE,  false, "5|1",              "5.0|0"),
            of(2,  "Рыбак с Лофотен",      HeroType.ACTIVE,  false, "2.5|2",            "1.5|1"),
            of(3,  "Скальд",               HeroType.BAFFER,  false, "1.25|3",           "4.5|1"),
            of(4,  "Охотница фьордов",     HeroType.PASSIVE, false, "6.25|3",           "1.35|2"),
            of(5,  "Щитоносица",           HeroType.PASSIVE, false, "3.125|4",          "4.05|2"),
            of(6,  "Берсерк",              HeroType.ACTIVE,  false, "1.5625|5",         "1.215|3"),
            of(7,  "Вёльва",               HeroType.ACTIVE,  false, "7.8125|5",         "3.645|3"),
            of(8,  "Гном Брокк",           HeroType.PASSIVE, false, "3.90625|6",        "1.0935|4"),
            of(9,  "Ульфхеднар",           HeroType.ACTIVE,  false, "1.953125|7",       "3.2805|4"),
            of(10, "Капитан драккара",     HeroType.PASSIVE, false, "9.765625|7",       "9.8415|4"),
            of(11, "Эйнхерий",             HeroType.BAFFER,  true,  "4.8828125|8",      "2.95245|5"),
            of(12, "Валькирия-новобранец", HeroType.PASSIVE, false, "2.44140625|9",     "8.85735|5"),
            of(13, "Хульдра",              HeroType.ACTIVE,  false, "1.220703125|10",   "2.657205|6"),
            of(14, "Драуг",                HeroType.PASSIVE, false, "6.103515625|10",   "7.971615|6"),
            of(15, "Ледяной ётун",         HeroType.REBIRTH, false, "3.0517578125|11",  "2.3914845|7")
    );

    public static SimHero byId(int id) {
        return CATALOG.get(id - 1);
    }
}
