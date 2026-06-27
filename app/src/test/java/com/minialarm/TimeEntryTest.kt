package com.minialarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Exhaustive unit tests for the pure entry/validation logic (SPEC.md §2.2/§2.3). */
class TimeEntryTest {

    private fun type(s: String): TimeEntry {
        val e = TimeEntry()
        for (c in s) e.append(c)
        return e
    }

    // ---- empty / initial state ----

    @Test fun initialState() {
        val e = TimeEntry()
        assertEquals("", e.raw)
        assertEquals(0, e.length)
        assertFalse(e.isComplete)
        assertNull(e.hour)
        assertNull(e.minute)
        assertEquals("––:––", e.display())
    }

    // ---- display formatting at every length ----

    @Test fun displayProgresses() {
        val e = TimeEntry()
        assertEquals("––:––", e.display())
        e.append('0'); assertEquals("0–:––", e.display())
        e.append('9'); assertEquals("09:––", e.display())
        e.append('3'); assertEquals("09:3–", e.display())
        e.append('0'); assertEquals("09:30", e.display())
    }

    // ---- a normal complete entry ----

    @Test fun completeEntry() {
        val e = type("0930")
        assertTrue(e.isComplete)
        assertEquals(9, e.hour)
        assertEquals(30, e.minute)
        assertEquals("09:30", e.display())
    }

    @Test fun explicitLeadingZeros() {
        val e = type("0005")
        assertEquals(0, e.hour)
        assertEquals(5, e.minute)
        assertEquals("00:05", e.display())
    }

    // ---- boundaries ----

    @Test fun midnight() {
        val e = type("0000")
        assertEquals(0, e.hour)
        assertEquals(0, e.minute)
        assertEquals("00:00", e.display())
    }

    @Test fun lastMinuteOfDay() {
        val e = type("2359")
        assertEquals(23, e.hour)
        assertEquals(59, e.minute)
        assertEquals("23:59", e.display())
    }

    // ---- rejection rules ----

    @Test fun hourTensRejectsThreeAndUp() {
        val e = TimeEntry()
        assertFalse(e.append('3'))
        assertFalse(e.append('9'))
        assertEquals(0, e.length)
        assertTrue(e.append('2'))
        assertEquals("2", e.raw)
    }

    @Test fun hourOnesAfterTwoRejectsFourAndUp() {
        val e = type("2")
        assertFalse(e.append('4'))
        assertFalse(e.append('9'))
        assertEquals("2", e.raw)
        assertTrue(e.append('3')) // 23 is valid
        assertEquals("23", e.raw)
    }

    @Test fun hourOnesAfterOneAcceptsNine() {
        val e = type("1")
        assertTrue(e.append('9')) // 19:xx valid
        assertEquals("19", e.raw)
    }

    @Test fun minuteTensRejectsSixAndUp() {
        val e = type("12")
        assertFalse(e.append('6'))
        assertFalse(e.append('9'))
        assertEquals("12", e.raw)
        assertTrue(e.append('5')) // 5x minutes valid
        assertEquals("125", e.raw)
    }

    @Test fun minuteOnesAcceptsAllDigits() {
        for (d in '0'..'9') {
            val e = type("125")
            assertTrue("minute ones should accept $d", e.append(d))
            assertEquals(50 + (d - '0'), e.minute)
        }
    }

    @Test fun rejectsNonDigits() {
        val e = TimeEntry()
        assertFalse(e.append('a'))
        assertFalse(e.append(':'))
        assertFalse(e.append(' '))
        assertEquals(0, e.length)
    }

    @Test fun rejectsFifthDigit() {
        val e = type("2359")
        assertFalse(e.append('0'))
        assertEquals("2359", e.raw)
        assertEquals(4, e.length)
    }

    // ---- backspace ----

    @Test fun backspaceRemovesLast() {
        val e = type("093")
        assertTrue(e.backspace())
        assertEquals("09", e.raw)
        assertEquals("09:––", e.display())
    }

    @Test fun backspaceOnEmptyIsNoOp() {
        val e = TimeEntry()
        assertFalse(e.backspace())
        assertEquals(0, e.length)
    }

    @Test fun backspaceThenReentryRevalidates() {
        // After deleting the hour-tens '2', the previously-blocked '9' for hour
        // ones should follow the new context.
        val e = type("23")
        e.backspace()              // "2"
        e.backspace()              // ""
        assertTrue(e.append('1'))  // "1"
        assertTrue(e.append('9'))  // "19" now valid
        assertEquals("19", e.raw)
    }

    // ---- exhaustive: every valid 24h time is reachable and every complete entry is valid ----

    @Test fun allValidTimesAreEnterable() {
        for (h in 0..23) for (m in 0..59) {
            val s = "%02d%02d".format(h, m)
            val e = type(s)
            assertTrue("should complete $s", e.isComplete)
            assertEquals(h, e.hour)
            assertEquals(m, e.minute)
            assertEquals("%02d:%02d".format(h, m), e.display())
        }
    }

    @Test fun everyCompleteEntryIsValidTime() {
        // Drive the entry with all 10 digits at each step; whatever it accepts
        // must always form a valid 24h time.
        fun explore(e: TimeEntry) {
            if (e.isComplete) {
                assertTrue(e.hour!! in 0..23)
                assertTrue(e.minute!! in 0..59)
                return
            }
            for (d in '0'..'9') {
                val snapshot = e.raw
                if (e.append(d)) {
                    explore(e)
                    // restore
                    e.clear()
                    for (c in snapshot) e.append(c)
                }
            }
        }
        explore(TimeEntry())
    }
}
