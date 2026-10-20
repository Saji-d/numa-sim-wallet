package com.sajid.numa

import org.junit.Assert.*
import org.junit.Test

class FormatTest {
    private val now = 1_800_000_000_000L
    private val minute = 60_000L
    private val hour = 3_600_000L

    // ---- formatRelative ----

    @Test fun formatRelative_justNow() {
        assertEquals("Just now", formatRelative(now, now))
        assertEquals("Just now", formatRelative(now - 59_999L, now))
    }

    @Test fun formatRelative_minutes() {
        assertEquals("1 min ago", formatRelative(now - minute, now))
        assertEquals("5 min ago", formatRelative(now - 5 * minute - 30_000L, now))
        assertEquals("59 min ago", formatRelative(now - hour + 1, now))
    }

    @Test fun formatRelative_hours() {
        assertEquals("1 h ago", formatRelative(now - hour, now))
        assertEquals("23 h ago", formatRelative(now - DAY_MS + 1, now))
    }

    @Test fun formatRelative_yesterday() {
        assertEquals("Yesterday", formatRelative(now - DAY_MS, now))
        assertEquals("Yesterday", formatRelative(now - 2 * DAY_MS + 1, now))
    }

    @Test fun formatRelative_olderFallsBackToDate() {
        val at = now - 10 * DAY_MS
        assertEquals(formatDate(at), formatRelative(at, now))
        val twoDays = now - 2 * DAY_MS
        assertEquals(formatDate(twoDays), formatRelative(twoDays, now))
    }

    // ---- formatDuration / formatUsd ----

    @Test fun formatDuration_cases() {
        assertEquals("0:00", formatDuration(0))
        assertEquals("0:05", formatDuration(5))
        assertEquals("1:15", formatDuration(75))
        assertEquals("10:00", formatDuration(600))
        assertEquals("60:00", formatDuration(3600))
    }

    @Test fun formatUsd_cases() {
        assertEquals("$3.50", formatUsd(3.5))
        assertEquals("$16.00", formatUsd(16.0))
        assertEquals("$0.00", formatUsd(0.0))
        assertEquals("$9.50", formatUsd(9.5))
    }

    // ---- daysLeft ----

    @Test fun daysLeft_neverNegative() {
        assertEquals(0, daysLeft(now - 10 * DAY_MS, now))
        assertEquals(0, daysLeft(now - 1, now))
    }

    @Test fun daysLeft_roundsUpPartialDays() {
        assertEquals(0, daysLeft(now, now))
        assertEquals(1, daysLeft(now + 1, now))
        assertEquals(1, daysLeft(now + DAY_MS, now))
        assertEquals(2, daysLeft(now + DAY_MS + 1, now))
        assertEquals(30, daysLeft(now + 30 * DAY_MS, now))
    }

    // ---- isValidEmail ----

    @Test fun isValidEmail_valid() {
        assertTrue(isValidEmail("a@b.co"))
        assertTrue(isValidEmail("first.last+tag@sub.example.org"))
        assertTrue(isValidEmail("  sam@example.com  ")) // trimmed
    }

    @Test fun isValidEmail_invalid() {
        assertFalse(isValidEmail(""))
        assertFalse(isValidEmail("a@b"))
        assertFalse(isValidEmail("@b.com"))
        assertFalse(isValidEmail("a@.com"))
        assertFalse(isValidEmail("a b@c.com"))
        assertFalse(isValidEmail("a@b.c"))       // TLD must be at least 2 letters
        assertFalse(isValidEmail("plainaddress"))
    }

    // ---- isValidDialString ----

    @Test fun isValidDialString_valid() {
        assertTrue(isValidDialString("+1 555 0142"))
        assertTrue(isValidDialString("555-0142"))
        assertTrue(isValidDialString("+44 20-7946 0958"))
        assertTrue(isValidDialString("*123#"))
        assertTrue(isValidDialString("911"))
        assertTrue(isValidDialString("123456789012345"))   // 15 chars
    }

    @Test fun isValidDialString_invalid() {
        assertFalse(isValidDialString(""))
        assertFalse(isValidDialString("12"))                // too short
        assertFalse(isValidDialString("+"))
        assertFalse(isValidDialString("abc"))
        assertFalse(isValidDialString("5550142x"))
        assertFalse(isValidDialString("+44+1555"))
        assertFalse(isValidDialString("1234567890123456"))  // 16 chars
        assertFalse(isValidDialString("   "))
    }
}

class CreditsTextTest {
    @org.junit.Test fun singular() = org.junit.Assert.assertEquals("1 credit", creditsText(1))
    @org.junit.Test fun plural() = org.junit.Assert.assertEquals("5 credits", creditsText(5))
    @org.junit.Test fun zero() = org.junit.Assert.assertEquals("0 credits", creditsText(0))
}
