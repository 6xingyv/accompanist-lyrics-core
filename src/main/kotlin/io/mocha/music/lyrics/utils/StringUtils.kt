package io.mocha.music.lyrics.utils

fun String.isDigitsOnly(): Boolean {
    return this.all { it.isDigit() }
}

fun String.parseAsTime(): Int {

    val parts = this.split(":")
    val time= when (parts.size) {
        3 -> {
            val hours = parts[0].toInt() * 3600 * 1000
            val minutes = parts[1].toInt() * 60 * 1000
            val secondsAndMillis = parts[2].split(".")
            val seconds = secondsAndMillis[0].toInt() * 1000
            val millis =
                if (secondsAndMillis.size > 1) secondsAndMillis[1].toInt()  else 0
            println(parts[0]+ ":"+parts[1]+":"+parts[2])
            hours + minutes + seconds + millis
        }

        2 -> {
            val minutes = parts[0].toInt() * 60 * 1000
            val secondsAndMillis = parts[1].split(".")
            val seconds = secondsAndMillis[0].toInt() * 1000
            val millis =
                if (secondsAndMillis.size > 1) secondsAndMillis[1].toInt()  else 0
            minutes + seconds + millis
        }

        else -> 0
    }
    return time
}