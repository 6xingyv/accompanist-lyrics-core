package com.mocharealm.accompanist.lyrics.core.utils

import com.mocharealm.accompanist.lyrics.core.model.karaoke.PhoneticLevel

interface PhoneticProvider {
    val phoneticLevel: PhoneticLevel
    fun getPhonetic(string: String): String

    /** Language-aware overload. Existing providers keep working through this default. */
    fun getPhonetic(string: String, languageTag: String?): String = getPhonetic(string)
}
