package com.mocharealm.accompanist.lyrics.core.model.karaoke

import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine

/**
 * Represents a line of lyrics with syllable-level timing information (Karaoke).
 *
 * @property syllables The list of syllables in this line.
 * @property translation The translation of the line, if available.
 * @property isAccompaniment True if this line is a background vocal or accompaniment.
 * @property alignment The alignment of the line (e.g., Start, End) for multi-singer scenarios.
 * @property start The start time of the line in milliseconds.
 * @property end The end time of the line in milliseconds.
 * @property phonetic Optional phonetic (romanized) representation of the lyrics.
 */
data class KaraokeLine(
    val syllables: List<KaraokeSyllable>,
    val translation: String?,
    val isAccompaniment: Boolean,
    val alignment: KaraokeAlignment,
    override val start: Int,
    override val end: Int,
    val phonetic: String? = null,
) : ISyncedLine {

    init {
        require(end >= start)
    }

    override val duration = end - start

    /**
     * Calculates the progress of the current line based on the current time.
     *
     * @param current The current playback time in milliseconds.
     * @return A float value between 0.0 and 1.0 representing the progress.
     */
    fun progress(current: Int): Float {
        return when {
            current < start -> 0f
            isFocused(current) -> (current - start).toFloat() / duration
            current > end -> 1f
            else -> 0f
        }.coerceIn(0f, 1f)
    }

    /**
     * Checks if the line is currently "focused" or active based on the current time.
     * Starts and ends slightly earlier/later for accompaniment lines to keep them visible longer.
     *
     * @param current The current playback time in milliseconds.
     * @return True if the line is considered active.
     */
    fun isFocused(current: Int): Boolean {
        if (!isAccompaniment)
            return current in start..end
        else
            return current in (start - 800)..(end + 800)
    }

    fun List<KaraokeSyllable>.contents(): String {
        return this.joinToString("") { it.content }
    }
}

fun SyncedLine.toKaraokeLine(): KaraokeLine {
    return KaraokeLine(
        syllables = listOf(
            KaraokeSyllable(
                this.content,
                this.start,
                this.end
            )
        ),
        translation = this.translation,
        isAccompaniment = false,
        alignment = KaraokeAlignment.Unspecified,
        start = this.start,
        end = this.end
    )
}