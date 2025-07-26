package com.mocharealm.accompanist.lyrics.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class StringUtilsTest {
    @Test
    fun test3DigitsMillsParse() {
        val time = "00:00.123".parseAsTime()
        assertEquals(123, time)
    }

    @Test
    fun test2DigitsMillsParse() {
        val time = "00:00.12".parseAsTime()
        assertEquals(120, time)
    }

    @Test
    fun test1DigitMillsParse() {
        val time = "00:00.1".parseAsTime()
        assertEquals(100, time)
    }

    @Test
    fun testNoMillsParse() {
        val time = "00:00".parseAsTime()
        assertEquals(0, time)
    }

    @Test
    fun testInvalidTimeParse() {
        val time = "invalid".parseAsTime()
        assertEquals(0, time)
    }

    @Test
    fun testNoHoursAndMinutesParse() {
        val time = "00.123".parseAsTime()
        assertEquals(123, time)
    }

    @Test
    fun testNoHoursParse() {
        val time = "00:00.123".parseAsTime()
        assertEquals(123, time)
    }
}