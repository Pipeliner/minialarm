package com.minialarm

import android.content.Intent
import android.provider.AlarmClock
import android.widget.Button
import android.widget.TextView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController

/** Local integration tests for the activity wiring, run on the JVM via Robolectric. */
@RunWith(RobolectricTestRunner::class)
class MainActivityRobolectricTest {

    private fun launch(): ActivityController<MainActivity> =
        Robolectric.buildActivity(MainActivity::class.java).setup()

    private fun MainActivity.display() = findViewById<TextView>(R.id.display).text.toString()

    private fun MainActivity.tap(id: Int) = findViewById<Button>(id).performClick()

    private val digitKey = mapOf(
        '0' to R.id.key0, '1' to R.id.key1, '2' to R.id.key2, '3' to R.id.key3,
        '4' to R.id.key4, '5' to R.id.key5, '6' to R.id.key6, '7' to R.id.key7,
        '8' to R.id.key8, '9' to R.id.key9
    )

    private fun MainActivity.type(s: String) = s.forEach { tap(digitKey.getValue(it)) }

    @Test fun startsWithPlaceholder() {
        val a = launch().get()
        assertEquals("––:––", a.display())
    }

    @Test fun displayUpdatesAsDigitsAreTapped() {
        val a = launch().get()
        a.tap(R.id.key0); assertEquals("0–:––", a.display())
        a.tap(R.id.key9); assertEquals("09:––", a.display())
        a.tap(R.id.key3); assertEquals("09:3–", a.display())
        a.tap(R.id.key0); assertEquals("09:30", a.display())
    }

    @Test fun invalidDigitTapIsIgnored() {
        val a = launch().get()
        a.tap(R.id.key9) // invalid for hour-tens
        assertEquals("––:––", a.display())
    }

    @Test fun backspaceUpdatesDisplay() {
        val a = launch().get()
        a.type("093")
        a.tap(R.id.keyBack)
        assertEquals("09:––", a.display())
    }

    @Test fun enterWithFourDigitsSetsAlarmAndFinishes() {
        val controller = launch()
        val a = controller.get()
        a.type("0730")
        a.tap(R.id.keyEnter)

        val started: Intent? = shadowOf(a).nextStartedActivity
        assertTrue("an activity should have been started", started != null)
        assertEquals(AlarmClock.ACTION_SET_ALARM, started!!.action)
        assertEquals(7, started.getIntExtra(AlarmClock.EXTRA_HOUR, -1))
        assertEquals(30, started.getIntExtra(AlarmClock.EXTRA_MINUTES, -1))
        assertTrue(started.getBooleanExtra(AlarmClock.EXTRA_SKIP_UI, false))
        assertTrue("activity should be finishing", a.isFinishing)
    }

    @Test fun enterWithIncompleteEntryDoesNothing() {
        val a = launch().get()
        a.type("07") // only two digits
        a.tap(R.id.keyEnter)

        assertNull("no alarm intent expected", shadowOf(a).nextStartedActivity)
        assertFalse("activity should stay open", a.isFinishing)
        assertEquals("07:––", a.display())
    }

    @Test fun midnightBoundary() {
        val a = launch().get()
        a.type("0000")
        a.tap(R.id.keyEnter)
        val started = shadowOf(a).nextStartedActivity!!
        assertEquals(0, started.getIntExtra(AlarmClock.EXTRA_HOUR, -1))
        assertEquals(0, started.getIntExtra(AlarmClock.EXTRA_MINUTES, -1))
    }

    @Test fun lastMinuteBoundary() {
        val a = launch().get()
        a.type("2359")
        a.tap(R.id.keyEnter)
        val started = shadowOf(a).nextStartedActivity!!
        assertEquals(23, started.getIntExtra(AlarmClock.EXTRA_HOUR, -1))
        assertEquals(59, started.getIntExtra(AlarmClock.EXTRA_MINUTES, -1))
    }
}
