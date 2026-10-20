package com.sajid.numa

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** "Just now", "5 min ago", "Yesterday", or a short date. */
fun formatRelative(at: Long, now: Long = System.currentTimeMillis()): String {
    val diff = now - at
    return when {
        diff < 60_000L -> "Just now"
        diff < 3_600_000L -> "${diff / 60_000L} min ago"
        diff < DAY_MS -> "${diff / 3_600_000L} h ago"
        diff < 2 * DAY_MS -> "Yesterday"
        else -> formatDate(at)
    }
}

fun formatDate(at: Long): String = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(at))

/** 75 -> "1:15". */
fun formatDuration(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)

/** "1 credit" / "5 credits". */
fun creditsText(n: Int): String = if (n == 1) "1 credit" else "$n credits"

fun formatUsd(v: Double): String = "$" + "%.2f".format(Locale.US, v)

/** Whole days left until [at] (never negative). */
fun daysLeft(at: Long, now: Long = System.currentTimeMillis()): Int = maxOf(0L, (at - now + DAY_MS - 1) / DAY_MS).toInt()

private val EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
fun isValidEmail(s: String) = EMAIL.matches(s.trim())

/** Digits with an optional leading +, 3 to 15 digits (E.164 upper bound). Spaces and dashes are ignored. */
fun isValidDialString(s: String): Boolean {
    val t = s.filter { it != ' ' && it != '-' }
    return Regex("^\\+?[0-9*#]{3,15}$").matches(t)
}
