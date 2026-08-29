package com.armsone.denimdex.core.domain

import java.util.Locale
import kotlin.math.floor
import kotlin.math.max

object CountdownFormatter {
    const val QUICK_VALUE_TIMEOUT_SECONDS: Int = 90

    fun remainingSeconds(elapsed: Double): Int {
        val wholeElapsed = floor(elapsed).toInt()
        return max(0, QUICK_VALUE_TIMEOUT_SECONDS - wholeElapsed)
    }

    fun formatMinutesSeconds(totalSeconds: Int): String {
        if (totalSeconds <= 0) return "0:00"
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%d:%02d", minutes, seconds)
    }

    fun progressFraction(elapsed: Double): Float {
        val remaining = remainingSeconds(elapsed)
        return (remaining / QUICK_VALUE_TIMEOUT_SECONDS.toDouble()).toFloat().coerceIn(0.0f, 1.0f)
    }

    fun isExpired(elapsed: Double): Boolean {
        return elapsed >= QUICK_VALUE_TIMEOUT_SECONDS.toDouble()
    }
}
