package com.mocharealm.accompanist.lyrics.core.model.karaoke

import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine

/**
 * Represents a line of lyrics with syllable-level timing information (Karaoke).
 *
 * @property syllables The list of syllables in this line.
 * @property translation The translation of the line, if available.
 * @property alignment The alignment of the line (e.g., Start, End) for multi-singer scenarios.
 * @property start The start time of the line in milliseconds.
 * @property end The end time of the line in milliseconds.
 * @property phonetic Optional phonetic (romanized) representation of the lyrics.
 */
sealed interface KaraokeLine : ISyncedLine {
    val syllables: List<KaraokeSyllable>
    val translation: String?
    val alignment: KaraokeAlignment
    override val start: Int
    override val end: Int
    val phonetic: String?


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
        return current in start..end
    }

    fun List<KaraokeSyllable>.contents(): String {
        return this.joinToString("") { it.content }
    }

    data class MainKaraokeLine(
        override val syllables: List<KaraokeSyllable>,
        override val translation: String?,
        override val alignment: KaraokeAlignment,
        override val start: Int,
        override val end: Int,
        override val phonetic: String? = null,
        val accompanimentLines: List<AccompanimentKaraokeLine>? = null
    ) : KaraokeLine {
        init {
            require(end >= start)
        }

        override val duration = end - start
    }

    data class AccompanimentKaraokeLine(
        override val syllables: List<KaraokeSyllable>,
        override val translation: String?,
        override val alignment: KaraokeAlignment,
        override val start: Int,
        override val end: Int,
        override val phonetic: String? = null
    ) : KaraokeLine {
        init {
            require(end >= start)
        }

        override val duration = end - start
    }
}
