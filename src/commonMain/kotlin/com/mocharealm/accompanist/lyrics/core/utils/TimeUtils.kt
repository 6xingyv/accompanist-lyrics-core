package com.mocharealm.accompanist.lyrics.core.utils

internal fun String.isDigitsOnly(): Boolean {
    return this.all { it.isDigit() }
}

internal fun String.parseAsTime(): Int {
    if (this.isEmpty()) return 0

    fun parseSecondsAndMillis(part: String): Int {
        val dotIndex = part.indexOf('.')
        if (dotIndex == -1) {
            return (part.toIntOrNull() ?: 0) * 1000
        }

        val seconds = part.substring(0, dotIndex).toIntOrNull()?.times(1000) ?: 0
        val millisStr = part.substring(dotIndex + 1)
        
        if (millisStr.isEmpty()) return seconds
        
        val normalizedMillisStr = when (millisStr.length) {
            1 -> millisStr + "00"
            2 -> millisStr + "0"
            3 -> millisStr
            else -> millisStr.substring(0, 3)
        }
        val millis = normalizedMillisStr.toIntOrNull() ?: 0
        return seconds + millis
    }

    return try {
        val firstColon = this.indexOf(':')
        if (firstColon == -1) {
            return parseSecondsAndMillis(this)
        }

        val lastColon = this.lastIndexOf(':')
        if (firstColon == lastColon) {
            // Format: MM:SS.ms
            val minutes = this.substring(0, firstColon).toIntOrNull()?.times(60 * 1000) ?: 0
            val secondsAndMillis = parseSecondsAndMillis(this.substring(firstColon + 1))
            minutes + secondsAndMillis
        } else {
            // Format: HH:MM:SS.ms
            val hours = this.substring(0, firstColon).toIntOrNull()?.times(3600 * 1000) ?: 0
            val minutes = this.substring(firstColon + 1, lastColon).toIntOrNull()?.times(60 * 1000) ?: 0
            val secondsAndMillis = parseSecondsAndMillis(this.substring(lastColon + 1))
            hours + minutes + secondsAndMillis
        }
    } catch (_: Exception) {
        0
    }
}

internal fun Int.toTimeFormattedString(): String {
    val totalMillis = this
    if (totalMillis < 0) return "00:00.000"

    val totalSeconds = totalMillis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val millis = totalMillis % 1000

    val m = minutes.toString().padStart(2, '0')
    val s = seconds.toString().padStart(2, '0')
    val ms = millis.toString().padStart(3, '0')

    return "$m:$s.$ms"
}