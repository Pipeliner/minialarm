package com.minialarm

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.AlarmClock
import android.widget.Button
import android.widget.TextView

/**
 * The whole app: a numeric keypad over a display. Four digits + Enter sets a
 * system alarm and closes. See SPEC.md.
 */
class MainActivity : Activity() {

    private val entry = TimeEntry()
    private lateinit var display: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        display = findViewById(R.id.display)

        // Digit keys.
        val digitIds = intArrayOf(
            R.id.key0, R.id.key1, R.id.key2, R.id.key3, R.id.key4,
            R.id.key5, R.id.key6, R.id.key7, R.id.key8, R.id.key9
        )
        for (digit in 0..9) {
            findViewById<Button>(digitIds[digit]).setOnClickListener { onDigit('0' + digit) }
        }

        findViewById<Button>(R.id.keyBack).setOnClickListener { onBackspace() }
        findViewById<Button>(R.id.keyEnter).setOnClickListener { onEnter() }

        render()
    }

    private fun onDigit(d: Char) {
        if (entry.append(d)) render()
    }

    private fun onBackspace() {
        if (entry.backspace()) render()
    }

    private fun onEnter() {
        if (!entry.isComplete) return
        setAlarm(entry.hour!!, entry.minute!!)
        finish()
    }

    private fun render() {
        display.text = entry.display()
    }

    private fun setAlarm(hour: Int, minute: Int) {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
        }
        // Best effort: don't crash if no clock app is present to handle it.
        try {
            startActivity(intent)
        } catch (_: android.content.ActivityNotFoundException) {
        }
    }
}
