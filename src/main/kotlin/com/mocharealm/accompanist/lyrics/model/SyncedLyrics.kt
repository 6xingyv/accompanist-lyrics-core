package com.mocharealm.accompanist.lyrics.model

data class SyncedLyrics(
    val lines: List<ISyncedLine>,
    val title: String = "",
    val id: String = "0",
    val artists: List<String>? = listOf<String>(),
) {
    /**
     * Gets the index of the first line that should be highlighted at the given time.
     *
     * This function performs a binary search on the `lines` list to find the line
     * whose time range (start to end) includes the given `time`.
     *
     * @param time The current time in milliseconds.
     * @return The index of the first line to be highlighted.
     *         If `lines` is empty, returns 0.
     *         If no line contains the given `time`, it returns the index of the line
     *         that would immediately follow the `time`, or the size of the `lines` list
     *         if `time` is after all lines.
     */
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
            else -> lines.size
        }
    }
}
