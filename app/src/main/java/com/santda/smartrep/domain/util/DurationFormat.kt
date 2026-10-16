package com.santda.smartrep.domain.util



/**
 * Converts between canonical seconds (stored) and human text.
 *
 * Format:  59 -> "0:59", 90 -> "1:30", 3725 -> "1:02:05"
 * Parse accepts: "90" (seconds), "1:30" (m:ss), "1:02:05" (h:mm:ss).
 * Only the first segment may exceed 59, so "90:00" is valid and "1:75" is not.
 */
object DurationFormat {

    fun format(totalSeconds: Long): String {
        require(totalSeconds >= 0) { "totalSeconds must be >= 0" }
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        val mm = minutes.toString().padStart(2, '0')
        val ss = seconds.toString().padStart(2, '0')
        return if (hours > 0) "$hours:$mm:$ss" else "$minutes:$ss"
    }

    /** Returns total seconds, or null if [text] is not a valid duration. */
    fun parse(text: String): Long? {
        val parts = text.trim().split(':')
        if (parts.size !in 1..3) return null
        if (parts.any { it.isEmpty() || !it.all(Char::isDigit) }) return null

        val numbers = parts.map { it.toLongOrNull() ?: return null }
        if (numbers.drop(1).any { it >= 60 }) return null

        return when (numbers.size) {
            1 -> numbers[0]
            2 -> numbers[0] * 60 + numbers[1]
            else -> numbers[0] * 3600 + numbers[1] * 60 + numbers[2]
        }
    }
}