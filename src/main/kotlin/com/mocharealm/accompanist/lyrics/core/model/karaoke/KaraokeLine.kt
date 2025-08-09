package com.mocharealm.accompanist.lyrics.core.model.karaoke

import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine

data class KaraokeLine(
    val syllables: List<KaraokeSyllable>,
    val translation: String?,
    val isAccompaniment: Boolean,
    val alignment: KaraokeAlignment,
    override val start: Int,
    override val end: Int,
) : ISyncedLine {

    init {
        require(end >= start)
    }

    override val duration = end - start

    fun progress(current: Int): Float {
        return when {
            current < start -> 0f
            isFocused(current) -> (current - start).toFloat() / duration
            current > end -> 1f
            else -> 0f
        }.coerceIn(0f, 1f)
    }

    fun isFocused(current: Int): Boolean {
        if (!isAccompaniment)
            return current in start..end
        else
            return current in (start - 800)..(end + 800)
    }
}

fun SyncedLine.toKaraokeLine(): KaraokeLine {
    return _root_ide_package_.com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine(
        syllables = listOf(
            _root_ide_package_.com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable(
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