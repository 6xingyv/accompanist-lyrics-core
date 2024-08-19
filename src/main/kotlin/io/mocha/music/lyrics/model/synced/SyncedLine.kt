package io.mocha.music.lyrics.model.synced

import io.mocha.music.lyrics.model.ISyncedLine

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
