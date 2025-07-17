package com.mocharealm.accompanist.lyrics.model

data class SyncedLyrics(
    val lines: List<ISyncedLine>,
    val title: String = "",
    val id: String = "0",
    val artists: List<String>? = listOf<String>(),
) {
    fun getCurrentFirstHighlightLineIndexByTime(time: Int): Int {
        if (lines.isEmpty()) return 0

        var low = 0
        var high = lines.size - 1

        while (low <= high) {
            val mid = (low + high) / 2
            val line = lines[mid]

            when {
                time in line.start..line.end -> return mid
                time < line.start -> high = mid - 1
                else -> low = mid + 1
            }
        }

        return when {
            low < lines.size -> low
            else -> lines.size - 2
        }
    }
}
