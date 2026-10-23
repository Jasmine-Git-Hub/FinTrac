package com.example.fintrac.core;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * An amount of money stored as whole centavos (long), so sums never pick up
 * floating-point errors like 0.1 + 0.2 = 0.30000000000000004.
 * Amounts are in the app's base currency (PHP); display conversion happens in the UI.
 */
public final class Money {

    public static final Money ZERO = new Money(0);

    private final long cents;

    private Money(long cents) {
        this.cents = cents;
    }

    public static Money ofCents(long cents) {
        return new Money(cents);
    }

    /** Rounds half-up to the nearest centavo. */
    public static Money ofPesos(double pesos) {
        return new Money(BigDecimal.valueOf(pesos).setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2).longValueExact());
    }

    /**
     * Parses legacy amount strings such as "1500", "1,500.50" or "₱1500.5".
     * Returns ZERO for null, blank or unparseable input.
     */
    public static Money parse(String text) {
        if (text == null) return ZERO;
        String clean = text.replaceAll("[^\\d.]", "");
        if (clean.isEmpty()) return ZERO;
        try {
            return new Money(new BigDecimal(clean).setScale(2, RoundingMode.HALF_UP)
                    .movePointRight(2).longValueExact());
        } catch (ArithmeticException | NumberFormatException e) {
            return ZERO;
        }
    }

    public long cents() {
        return cents;
    }

    /** For display/formatting APIs that still take a double. Don't do arithmetic on the result. */
    public double toPesos() {
        return cents / 100.0;
    }

    public Money plus(Money other) {
        return new Money(Math.addExact(cents, other.cents));
    }

    public Money minus(Money other) {
        return new Money(Math.subtractExact(cents, other.cents));
    }

    public boolean isGreaterThan(Money other) {
        return cents > other.cents;
    }

    public boolean isPositive() {
        return cents > 0;
    }

    /** Plain decimal string ("1500.50") used for the legacy string "amount" field. */
    public String toPlainString() {
        return BigDecimal.valueOf(cents, 2).toPlainString();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Money && ((Money) o).cents == cents;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(cents);
    }

    @Override
    public String toString() {
        return toPlainString();
    }
}
