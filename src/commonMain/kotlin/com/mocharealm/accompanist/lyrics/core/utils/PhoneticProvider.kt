package com.mocharealm.accompanist.lyrics.core.utils

import com.mocharealm.accompanist.lyrics.core.model.karaoke.PhoneticLevel

interface PhoneticProvider {
    val phoneticLevel: PhoneticLevel
    fun getPhonetic(string: String): String
}