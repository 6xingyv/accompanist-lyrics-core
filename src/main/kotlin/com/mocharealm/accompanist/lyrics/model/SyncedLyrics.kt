package com.mocharealm.accompanist.lyrics.model

data class SyncedLyrics(
    val lines: List<ISyncedLine>,
    val title: String = "",
    val id: String = "0",
    val artists: List<String>? = emptyList(),
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
        var resultIndex = lines.size

        while (low <= high) {
            val mid = low + (high - low) / 2
            val line = lines[mid]

            if (line.start > time) {
                resultIndex = mid
                high = mid - 1
            } else if (line.end < time) {
                low = mid + 1
            } else {
                resultIndex = mid
                high = mid - 1
            }
        }

        return if (resultIndex < lines.size && time in lines[resultIndex].start..lines[resultIndex].end) {
            resultIndex
        } else {
            low.coerceAtMost(lines.size)
        }
    }
}
