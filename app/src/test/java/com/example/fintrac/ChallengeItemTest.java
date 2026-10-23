package com.example.fintrac;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.fintrac.core.Money;

import org.junit.Test;

import java.util.Calendar;

public class ChallengeItemTest {

    private static long at(int day, int hour, int minute) {
        Calendar c = Calendar.getInstance();
        c.set(2026, Calendar.OCTOBER, day, hour, minute, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    @Test
    public void firstCheckInIsAlwaysAllowed() {
        assertTrue(ChallengeItem.canCheckIn(0, at(10, 3, 0)));
    }

    @Test
    public void newCheckInDayStartsAtEightAm() {
        long checkedIn = at(10, 9, 0);                              // 9:00 AM
        assertFalse(ChallengeItem.canCheckIn(checkedIn, at(10, 23, 0))); // same evening
        assertFalse(ChallengeItem.canCheckIn(checkedIn, at(11, 7, 59))); // next day, before 8
        assertTrue(ChallengeItem.canCheckIn(checkedIn, at(11, 8, 0)));   // next day, 8:00
    }

    @Test
    public void lateNightCheckInCountsForTheDayThatStartedAtEight() {
        long checkedIn = at(11, 2, 0);                              // 2 AM belongs to day 10's window
        assertFalse(ChallengeItem.canCheckIn(checkedIn, at(11, 7, 0)));
        assertTrue(ChallengeItem.canCheckIn(checkedIn, at(11, 8, 0)));
    }

    @Test
    public void savingsAreExactAndFinishAtTarget() {
        ChallengeItem item = new ChallengeItem("Daily", "", 0, 0, 0, 3, 100);
        assertEquals(3333, item.dailyAmount().cents());

        item.setCurrentDay(2);
        assertEquals(6666, item.savedSoFar().cents());

        item.setCurrentDay(3);
        assertTrue(item.isFinished());
        assertEquals(Money.ofPesos(100), item.savedSoFar());
    }

    @Test
    public void zeroDayChallengeDoesNotDivideByZero() {
        ChallengeItem item = new ChallengeItem("Broken", "", 0, 0, 0, 0, 100);
        assertEquals(Money.ZERO, item.dailyAmount());
    }
}
