package com.ragnarok.idle.math;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты BigNum. Ключевая цель — убедиться, что математика не ломается
 * на величинах, которые переполнили бы double (уровни 5000+).
 */
class BigNumTest {

    private static final double EPS = 1e-9;

    @Test
    @DisplayName("Создание из double и обратное чтение")
    void createFromDouble() {
        BigNum n = BigNum.of(12345);
        // 12345 = 1.2345e4
        assertEquals(4, n.getExponent());
        assertEquals(1.2345, n.getMantissa(), EPS);
    }

    @Test
    @DisplayName("Ноль обрабатывается корректно")
    void zeroHandling() {
        assertTrue(BigNum.of(0).isZero());
        assertTrue(BigNum.ZERO.isZero());
        assertEquals(BigNum.ZERO, BigNum.of(5).multiply(BigNum.ZERO));
    }

    @Test
    @DisplayName("Умножение маленьких чисел совпадает с обычным")
    void multiplySmall() {
        BigNum r = BigNum.of(20).multiply(BigNum.of(30));
        assertEquals(600.0, Math.pow(10, r.log10()), 1e-6);
    }

    @Test
    @DisplayName("Умножение огромных чисел не переполняется")
    void multiplyHuge() {
        // 10^200 × 10^200 = 10^400 — double бы дал Infinity
        BigNum a = BigNum.ofMantissaExp(1, 200);
        BigNum b = BigNum.ofMantissaExp(1, 200);
        BigNum r = a.multiply(b);
        assertEquals(400, r.getExponent());
        assertEquals(1.0, r.getMantissa(), EPS);
    }

    @Test
    @DisplayName("Деление больших чисел")
    void divideHuge() {
        BigNum a = BigNum.ofMantissaExp(6, 300);
        BigNum b = BigNum.ofMantissaExp(2, 100);
        BigNum r = a.divide(b);           // 3e200
        assertEquals(200, r.getExponent());
        assertEquals(3.0, r.getMantissa(), EPS);
    }

    @Test
    @DisplayName("Деление на ноль бросает исключение")
    void divideByZero() {
        assertThrows(ArithmeticException.class, () -> BigNum.of(5).divide(BigNum.ZERO));
    }

    @Test
    @DisplayName("Сложение близких по порядку чисел")
    void addClose() {
        BigNum r = BigNum.of(150).add(BigNum.of(50));
        assertEquals(200.0, Math.pow(10, r.log10()), 1e-6);
    }

    @Test
    @DisplayName("Сложение: пренебрежимо малое слагаемое игнорируется")
    void addNegligible() {
        BigNum big = BigNum.ofMantissaExp(1, 100);
        BigNum tiny = BigNum.of(5);
        assertEquals(big, big.add(tiny));   // 10^100 + 5 ≈ 10^100
    }

    @Test
    @DisplayName("Вычитание")
    void subtract() {
        BigNum r = BigNum.of(500).subtract(BigNum.of(200));
        assertEquals(300.0, Math.pow(10, r.log10()), 1e-6);
    }

    @Test
    @DisplayName("Степень: рост DPS героя dpsGrowth^level")
    void powerGrowth() {
        // 1.08^100 ≈ 2199.76
        BigNum r = BigNum.of(1.08).pow(100);
        assertEquals(2199.76, Math.pow(10, r.log10()), 1.0);
    }

    @Test
    @DisplayName("Степень до астрономических значений (множители вех на 50k)")
    void powerAstronomical() {
        // 25^5000 — колоссальное число; проверяем, что exponent огромный и без ошибок
        BigNum r = BigNum.of(25).pow(5000);
        // log10(25^5000) = 5000 × log10(25) ≈ 6989.7
        assertEquals(6989.7, r.log10(), 1.0);
    }

    @Test
    @DisplayName("HP на уровне 50k из полосной кривой (проверка масштаба)")
    void hpCurveScale() {
        // грубо: 1.006^9999 × 1.010^20000 × 1.020^15000 × 1.05^5000
        BigNum hp = BigNum.of(10)
                .multiply(BigNum.of(1.006).pow(9999))
                .multiply(BigNum.of(1.010).pow(20000))
                .multiply(BigNum.of(1.020).pow(15000))
                .multiply(BigNum.of(1.05).pow(5000));
        // ожидаем ~10^348 (см. GDD §3.2)
        assertTrue(hp.log10() > 340 && hp.log10() < 355,
                "HP на 50k должно быть ~10^348, а получилось 10^" + hp.log10());
    }

    @Test
    @DisplayName("Сравнение чисел разного порядка")
    void comparison() {
        BigNum small = BigNum.of(999);
        BigNum big = BigNum.ofMantissaExp(1, 50);
        assertTrue(big.gte(small));
        assertTrue(small.lt(big));
        assertTrue(BigNum.of(100).gte(BigNum.of(100)));
    }

    @Test
    @DisplayName("Сериализация в строку и обратно (для БД)")
    void storageRoundtrip() {
        BigNum original = BigNum.ofMantissaExp(3.14159, 12345);
        BigNum restored = BigNum.fromStorageString(original.toStorageString());
        assertEquals(original, restored);
    }

    @Test
    @DisplayName("Отображение с суффиксами K/M/B/T")
    void displayShortUnits() {
        assertEquals("999", BigNum.of(999).toDisplayString());
        assertEquals("1.50K", BigNum.of(1500).toDisplayString());
        assertEquals("2.00M", BigNum.of(2_000_000).toDisplayString());
        assertEquals("1.00B", BigNum.of(1_000_000_000).toDisplayString());
    }

    @Test
    @DisplayName("Отображение с буквенными суффиксами за пределами T")
    void displayLetterUnits() {
        // 10^15 = 1000T -> первый буквенный тир 'aa'
        String s = BigNum.ofMantissaExp(1, 15).toDisplayString();
        assertTrue(s.endsWith("aa"), "Ожидался суффикс aa, получено: " + s);
    }
}
