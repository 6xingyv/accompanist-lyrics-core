package com.mocharealm.accompanist.lyrics.model.synced

import com.mocharealm.accompanist.lyrics.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.model.karaoke.KaraokeLine

data class SyncedLine(
    val content: String,
    val translation: String?,
    override val start: Int,
    override val end: Int,
) : ISyncedLine {
    override val duration = end - start
    init {
        require(end >= start)
    }
}

data class UncheckedSyncedLine(
    val content: String,
    val translation: String?,
    override val start: Int,
    override val end: Int,
) : ISyncedLine {
    override val duration = (end - start).takeIf { it >= 0 } ?: 0

    fun toSyncedLine():SyncedLine {
        return SyncedLine(this.content,this.translation,this.start,this.end)
    }
}

fun KaraokeLine.toSyncedLine(): SyncedLine {
    return SyncedLine(
        content = this.syllables.joinToString("") { it.content }.trim(),
        translation = this.translation,
        start = this.start,
        end = this.end
    )
}
