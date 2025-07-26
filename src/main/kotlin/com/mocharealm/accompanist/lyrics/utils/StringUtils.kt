package com.mocharealm.accompanist.lyrics.utils

internal fun String.isDigitsOnly(): Boolean {
    return this.all { it.isDigit() }
}

internal fun String.parseAsTime(): Int {
    fun parseSecondsAndMillis(part: String): Int {
        val timeParts = part.split('.', limit = 2)
        val seconds = timeParts[0].toIntOrNull()?.times(1000) ?: 0

        // If there's no millisecond part, return seconds only.
        if (timeParts.size == 1) return seconds

        val millisStr = timeParts[1]
        // Pad the string to 3 digits on the right with '0'.
        // "4" -> "400", "45" -> "450", "456" -> "456"
        // Then take the first 3 characters to handle overly long inputs like "4567".
        val normalizedMillisStr = millisStr.padEnd(3, '0').substring(0, 3)
        val millis = normalizedMillisStr.toIntOrNull() ?: 0

        return seconds + millis
    }

    return try {
        val parts = this.split(":")
        when (parts.size) {
            3 -> { // Format: HH:MM:SS.ms
                val hours = parts[0].toIntOrNull()?.times(3600 * 1000) ?: 0
                val minutes = parts[1].toIntOrNull()?.times(60 * 1000) ?: 0
                val secondsAndMillis = parseSecondsAndMillis(parts[2])
                hours + minutes + secondsAndMillis
            }
            2 -> { // Format: MM:SS.ms
                val minutes = parts[0].toIntOrNull()?.times(60 * 1000) ?: 0
                val secondsAndMillis = parseSecondsAndMillis(parts[1])
                minutes + secondsAndMillis
            }
            1 -> { // Format: SS.ms
                parseSecondsAndMillis(parts[0])
            }
            else -> 0
        }
    } catch (_: Exception) {
        0
    }
}
