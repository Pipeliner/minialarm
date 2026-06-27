package com.minialarm

import android.app.Activity
import android.app.Instrumentation
import android.provider.AlarmClock
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** On-device / emulator instrumentation tests for the critical flows. */
@RunWith(AndroidJUnit4::class)
class MainActivityEspressoTest {

    @get:Rule
    val scenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Before fun setUp() {
        Intents.init()
        // Stub the outgoing alarm intent so no real clock app is launched.
        Intents.intending(hasAction(AlarmClock.ACTION_SET_ALARM))
            .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))
    }

    @After fun tearDown() {
        Intents.release()
    }

    @Test fun typingFourDigitsRendersAsHhMm() {
        onView(withId(R.id.key0)).perform(click())
        onView(withId(R.id.key7)).perform(click())
        onView(withId(R.id.key3)).perform(click())
        onView(withId(R.id.key0)).perform(click())
        onView(withId(R.id.display)).check(matches(withText("07:30")))
    }

    @Test fun enterFiresAlarmIntentWithCorrectExtras() {
        onView(withId(R.id.key0)).perform(click())
        onView(withId(R.id.key7)).perform(click())
        onView(withId(R.id.key3)).perform(click())
        onView(withId(R.id.key0)).perform(click())
        onView(withId(R.id.keyEnter)).perform(click())

        intended(
            allOf(
                hasAction(AlarmClock.ACTION_SET_ALARM),
                hasExtra(AlarmClock.EXTRA_HOUR, 7),
                hasExtra(AlarmClock.EXTRA_MINUTES, 30),
                hasExtra(AlarmClock.EXTRA_SKIP_UI, true)
            )
        )
    }

    @Test fun invalidFirstDigitIsIgnored() {
        onView(withId(R.id.key9)).perform(click()) // invalid hour-tens
        onView(withId(R.id.display)).check(matches(withText("––:––")))
    }
}
