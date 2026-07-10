package com.ragnarok.idle.math;

import java.util.Locale;
import java.util.Objects;

/**
 * Большое число в формате «мантисса × 10^exponent».
 * <p>
 * Зачем: в idle-кликере HP, золото, DPS и множители на уровнях 5000+ превышают
 * предел {@code double} (~1.8e308). Обычные {@code long}/{@code double} переполняются.
 * Этот класс хранит число как нормализованную мантиссу [1, 10) и целую экспоненту,
 * что позволяет работать с величинами до ~10^(9.2e18).
 * <p>
 * Класс неизменяемый (immutable): каждая операция возвращает новый экземпляр.
 * Аналог break_infinity.js / decimal-типов в Tap Titans и подобных играх.
 */
public final class BigNum implements Comparable<BigNum> {

    /** Мантисса, нормализована в диапазон [1, 10) для ненулевых значений. */
    private final double mantissa;
    /** Десятичная экспонента. */
    private final long exponent;

    public static final BigNum ZERO = new BigNum(0, 0);
    public static final BigNum ONE = new BigNum(1, 0);

    private BigNum(double mantissa, long exponent) {
        this.mantissa = mantissa;
        this.exponent = exponent;
    }

    // ---------- Фабрики ----------

    /** Из обычного double (напр. базовые значения из экономики). */
    public static BigNum of(double value) {
        if (value == 0 || Double.isNaN(value)) {
            return ZERO;
        }
        boolean negative = value < 0;
        double v = Math.abs(value);
        long exp = (long) Math.floor(Math.log10(v));
        double mant = v / Math.pow(10, exp);
        // защита от погрешностей log10 на границах (напр. mant вышла за [1,10))
        if (mant >= 10) { mant /= 10; exp++; }
        if (mant < 1)   { mant *= 10; exp--; }
        return new BigNum(negative ? -mant : mant, exp);
    }

    /** Прямое создание из мантиссы и экспоненты с последующей нормализацией. */
    public static BigNum ofMantissaExp(double mantissa, long exponent) {
        if (mantissa == 0) return ZERO;
        return normalize(mantissa, exponent);
    }

    private static BigNum normalize(double mantissa, long exponent) {
        if (mantissa == 0 || Double.isNaN(mantissa)) return ZERO;
        boolean negative = mantissa < 0;
        double m = Math.abs(mantissa);
        // подтягиваем мантиссу в [1,10)
        double log = Math.log10(m);
        long shift = (long) Math.floor(log);
        m = m / Math.pow(10, shift);
        exponent += shift;
        if (m >= 10) { m /= 10; exponent++; }
        if (m < 1)   { m *= 10; exponent--; }
        return new BigNum(negative ? -m : m, exponent);
    }

    // ---------- Арифметика ----------

    public BigNum multiply(BigNum other) {
        if (this.isZero() || other.isZero()) return ZERO;
        return normalize(this.mantissa * other.mantissa, this.exponent + other.exponent);
    }

    public BigNum multiply(double scalar) {
        return multiply(BigNum.of(scalar));
    }

    public BigNum divide(BigNum other) {
        if (other.isZero()) throw new ArithmeticException("Деление на ноль в BigNum");
        if (this.isZero()) return ZERO;
        return normalize(this.mantissa / other.mantissa, this.exponent - other.exponent);
    }

    public BigNum add(BigNum other) {
        if (this.isZero()) return other;
        if (other.isZero()) return this;
        // выравниваем по большей экспоненте; если разница огромна — меньшее игнорируется
        long diff = this.exponent - other.exponent;
        if (diff > 15) return this;      // other пренебрежимо мал
        if (diff < -15) return other;    // this пренебрежимо мал
        // приводим к общей экспоненте (меньшей из двух) и складываем мантиссы
        long baseExp = Math.min(this.exponent, other.exponent);
        double a = this.mantissa * Math.pow(10, this.exponent - baseExp);
        double b = other.mantissa * Math.pow(10, other.exponent - baseExp);
        return normalize(a + b, baseExp);
    }

    public BigNum subtract(BigNum other) {
        if (other.isZero()) return this;
        return this.add(new BigNum(-other.mantissa, other.exponent));
    }

    /** Возведение в целую степень (напр. dpsGrowth^level). */
    public BigNum pow(long power) {
        if (power == 0) return ONE;
        if (this.isZero()) return ZERO;
        // (m × 10^e)^p = m^p × 10^(e×p); m^p считаем через логарифм для больших p
        double logM = Math.log10(Math.abs(this.mantissa));
        double totalLog = logM * power + (double) this.exponent * power;
        long newExp = (long) Math.floor(totalLog);
        double newMant = Math.pow(10, totalLog - newExp);
        boolean negative = this.mantissa < 0 && (power % 2 != 0);
        return normalize(negative ? -newMant : newMant, newExp);
    }

    /**
     * Возведение в произвольную (в т.ч. дробную) степень — нужно для формул вида
     * {@code HP^0.97} (золото, GDD/экономика) и {@code HP^0.0009} (Слава, §3.9).
     * Отрицательное основание не поддерживается (в игровых величинах его не бывает).
     */
    public BigNum pow(double power) {
        if (this.isZero()) return power == 0 ? ONE : ZERO;
        if (this.mantissa < 0) {
            throw new ArithmeticException("pow(double) не поддерживает отрицательное основание");
        }
        double logM = Math.log10(this.mantissa);
        double totalLog = (logM + this.exponent) * power;
        long newExp = (long) Math.floor(totalLog);
        double newMant = Math.pow(10, totalLog - newExp);
        return normalize(newMant, newExp);
    }

    /**
     * Округление вниз до целого. За пределами точности double (exponent ≥ 15)
     * дробной части у величины уже физически нет — возвращаем как есть.
     */
    public BigNum floor() {
        if (this.exponent >= 15) return this;
        double plain = mantissa * Math.pow(10, exponent);
        return BigNum.of(Math.floor(plain));
    }

    /** Округление вверх до целого — для цен (магазин не продешевит). Аналогично {@link #floor()}. */
    public BigNum ceil() {
        if (this.exponent >= 15) return this;
        double plain = mantissa * Math.pow(10, exponent);
        return BigNum.of(Math.ceil(plain));
    }

    // ---------- Сравнение ----------

    @Override
    public int compareTo(BigNum other) {
        if (this.isZero() && other.isZero()) return 0;
        boolean thisNeg = this.mantissa < 0;
        boolean otherNeg = other.mantissa < 0;
        if (thisNeg != otherNeg) return thisNeg ? -1 : 1;
        int sign = thisNeg ? -1 : 1;
        if (this.exponent != other.exponent) {
            return Long.compare(this.exponent, other.exponent) * sign;
        }
        return Double.compare(this.mantissa, other.mantissa);
    }

    public boolean isZero() { return mantissa == 0; }
    public boolean gte(BigNum other) { return this.compareTo(other) >= 0; }
    public boolean lt(BigNum other) { return this.compareTo(other) < 0; }

    // ---------- Утилиты ----------

    /** log10 числа — удобно для отображения и балансных проверок. */
    public double log10() {
        if (isZero()) return Double.NEGATIVE_INFINITY;
        return exponent + Math.log10(Math.abs(mantissa));
    }

    public double getMantissa() { return mantissa; }
    public long getExponent() { return exponent; }

    /**
     * Форматирование для UI: маленькие числа — обычно, большие — с суффиксами
     * (K, M, B, T, aa, ab, ...) как принято в idle-играх.
     */
    public String toDisplayString() {
        if (isZero()) return "0";
        if (exponent < 3) {
            double plain = mantissa * Math.pow(10, exponent);
            long rounded = Math.round(plain);
            // допуск на float-артефакты нормализации (1.09×10^2 = 109.00000000000001 -> "109")
            if (Math.abs(plain - rounded) < 1e-9) {
                return String.valueOf(rounded);
            }
            return String.format(Locale.ROOT, "%.2f", plain);
        }
        String[] shortUnits = {"", "K", "M", "B", "T"};
        long tier = exponent / 3;
        if (tier < shortUnits.length) {
            double val = mantissa * Math.pow(10, exponent % 3);
            return String.format(Locale.ROOT, "%.2f%s", val, shortUnits[(int) tier]);
        }
        // за пределами T — буквенные суффиксы aa, ab, ... (idle-нотация)
        long idx = tier - shortUnits.length; // 0 -> aa
        char first = (char) ('a' + (idx / 26) % 26);
        char second = (char) ('a' + idx % 26);
        double val = mantissa * Math.pow(10, exponent % 3);
        return String.format(Locale.ROOT, "%.2f%c%c", val, first, second);
    }

    /** Сериализация для БД: строка "mantissa|exponent". */
    public String toStorageString() {
        return mantissa + "|" + exponent;
    }

    /** Десериализация из БД. */
    public static BigNum fromStorageString(String s) {
        if (s == null || s.isBlank()) return ZERO;
        String[] parts = s.split("\\|");
        return new BigNum(Double.parseDouble(parts[0]), Long.parseLong(parts[1]));
    }

    @Override
    public String toString() { return toDisplayString(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BigNum other)) return false;
        return Double.compare(mantissa, other.mantissa) == 0 && exponent == other.exponent;
    }

    @Override
    public int hashCode() { return Objects.hash(mantissa, exponent); }
}
