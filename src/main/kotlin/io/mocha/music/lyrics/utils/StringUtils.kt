package io.mocha.music.lyrics.utils

fun String.isDigitsOnly(): Boolean {
    return this.all { it.isDigit() }
}

fun String.parseAsTime(): Int {
    return try {
        val parts = this.split(":")
        when (parts.size) {
            3 -> {
                val hours = parts[0].toIntOrNull()?.let { it * 3600 * 1000 } ?: 0
                val minutes = parts[1].toIntOrNull()?.let { it * 60 * 1000 } ?: 0
                val (secondsStr, millisStr) = parts[2].split(".")
                val seconds = secondsStr.toIntOrNull()?.let { it * 1000 } ?: 0
                val millis = millisStr.toIntOrNull() ?: 0
                hours + minutes + seconds + millis
            }
            2 -> {
                val minutes = parts[0].toIntOrNull()?.let { it * 60 * 1000 } ?: 0
                val (secondsStr, millisStr) = parts[1].split(".")
                val seconds = secondsStr.toIntOrNull()?.let { it * 1000 } ?: 0
                val millis = millisStr.toIntOrNull() ?: 0
                minutes + seconds + millis
            }
            1 -> {
                val (secondsStr, millisStr) = parts[0].split(".")
                val seconds = secondsStr.toIntOrNull()?.let { it * 1000 } ?: 0
                val millis = millisStr.toIntOrNull() ?: 0
                seconds + millis
            }
            else -> 0
        }
    } catch (_: Exception) {
        0
    }
}
