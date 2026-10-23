package com.example.fintrac.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MoneyTest {

    @Test
    public void sumsHaveNoFloatingPointDrift() {
        Money total = Money.parse("0.10").plus(Money.parse("0.20"));
        assertEquals(30, total.cents());
        assertEquals("0.30", total.toPlainString());
    }

    @Test
    public void parsesLegacyAmountStrings() {
        assertEquals(150000, Money.parse("1500").cents());
        assertEquals(150050, Money.parse("1,500.50").cents());
        assertEquals(150050, Money.parse("₱1500.5").cents());
        assertEquals(1, Money.parse("0.005").cents()); // rounds half-up
    }

    @Test
    public void badInputIsZero() {
        assertEquals(Money.ZERO, Money.parse(null));
        assertEquals(Money.ZERO, Money.parse(""));
        assertEquals(Money.ZERO, Money.parse("abc"));
        assertEquals(Money.ZERO, Money.parse("1.2.3"));
        assertFalse(Money.parse("").isPositive());
    }

    @Test
    public void ofPesosRoundsToCentavos() {
        assertEquals(1999, Money.ofPesos(19.985).cents());
        assertEquals(1234, Money.ofPesos(12.34).cents());
        assertTrue(Money.ofPesos(0.01).isPositive());
    }
}
