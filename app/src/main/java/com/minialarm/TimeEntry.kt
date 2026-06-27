package com.minialarm

/**
 * Pure, Android-free model of the four-digit 24-hour time being entered.
 *
 * Digits are accepted one at a time. Input is constrained so that a complete
 * (four-digit) entry is always a valid 24-hour time: impossible digits for the
 * current position are rejected. See SPEC.md §2.2.
 */
class TimeEntry {

    private val digits = StringBuilder(MAX_DIGITS)

    /** The raw digits entered so far, e.g. "093". */
    val raw: String get() = digits.toString()

    /** Number of digits entered (0..4). */
    val length: Int get() = digits.length

    /** True once all four digits have been entered. */
    val isComplete: Boolean get() = digits.length == MAX_DIGITS

    /** Entered hour (0..23), or null until both hour digits are present. */
    val hour: Int?
        get() = if (digits.length >= 2) digits.substring(0, 2).toInt() else null

    /** Entered minute (0..59), or null until all four digits are present. */
    val minute: Int?
        get() = if (digits.length == MAX_DIGITS) digits.substring(2, 4).toInt() else null

    /**
     * Attempt to append [digit]. Returns true if it was accepted, false if it
     * was rejected (buffer full, not a digit, or invalid for this position).
     */
    fun append(digit: Char): Boolean {
        if (digits.length >= MAX_DIGITS) return false
        if (digit < '0' || digit > '9') return false
        if (!isAccepted(digits.length, digit)) return false
        digits.append(digit)
        return true
    }

    /** Remove the most recently entered digit. No-op if empty. Returns true if a digit was removed. */
    fun backspace(): Boolean {
        if (digits.isEmpty()) return false
        digits.deleteCharAt(digits.length - 1)
        return true
    }

    /** Clear all entered digits. */
    fun clear() {
        digits.setLength(0)
    }

    /**
     * Render as `HH:MM`, with the placeholder [PLACEHOLDER] for positions not
     * yet entered. E.g. "" -> "––:––", "09" -> "09:––", "0930" -> "09:30".
     */
    fun display(): String {
        val sb = StringBuilder(5)
        for (i in 0 until MAX_DIGITS) {
            if (i == 2) sb.append(':')
            sb.append(if (i < digits.length) digits[i] else PLACEHOLDER)
        }
        return sb.toString()
    }

    /** Whether [digit] is valid at zero-based [position] for a 24-hour time. */
    private fun isAccepted(position: Int, digit: Char): Boolean = when (position) {
        0 -> digit in '0'..'2'                                   // hour tens
        1 -> if (digits[0] == '2') digit in '0'..'3'             // hour ones (2x -> 0..3)
             else digit in '0'..'9'                              //           (0x/1x -> 0..9)
        2 -> digit in '0'..'5'                                   // minute tens
        3 -> digit in '0'..'9'                                   // minute ones
        else -> false
    }

    companion object {
        const val MAX_DIGITS = 4
        const val PLACEHOLDER = '–' // en dash "–"
    }
}
