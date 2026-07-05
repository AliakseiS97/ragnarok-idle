package com.ragnarok.idle.math;

/** Представление BigNum в ответах API: точные mantissa/exponent + готовая строка для UI. */
public record BigNumDto(double mantissa, long exponent, String display) {

    public static BigNumDto from(BigNum value) {
        return new BigNumDto(value.getMantissa(), value.getExponent(), value.toDisplayString());
    }
}
