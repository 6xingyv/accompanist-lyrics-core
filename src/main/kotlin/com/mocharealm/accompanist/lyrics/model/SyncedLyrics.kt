package com.mocharealm.accompanist.lyrics.model

/**
 * Represents synced lyrics for a song.
 *
 * @property lines A list of [ISyncedLine] objects, each representing a line of lyrics with timing information.
 * @property title The title of the song. Defaults to an empty string.
 * @property id The unique identifier for the lyrics. Defaults to "0".
 * @property artists A list of artists who performed the song. Defaults to an empty list.
 */
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

        val searchResult = lines.binarySearchBy(key = time) { line ->
            when {
                time < line.start -> 1
                time > line.end -> -1
                else -> 0
            }
        }

        return if (searchResult >= 0) {
            // Exact match found (time is within line.start..line.end)
            searchResult
        } else {
            // Not found, searchResult = (-(insertion point) - 1)
            // We need the insertion point: -(searchResult + 1)
            // This is the index where the element would be inserted to maintain order.
            // This corresponds to your original logic: "when low < lines.size -> low else -> lines.size"
            val insertionPoint = -(searchResult + 1)
            insertionPoint
        }
    }
}
