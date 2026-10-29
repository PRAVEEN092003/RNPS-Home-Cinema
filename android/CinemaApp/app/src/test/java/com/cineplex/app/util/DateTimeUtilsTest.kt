package com.cineplex.app.util

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class DateTimeUtilsTest {

    @Test
    fun testParseLocalDateTime_validIso() {
        val dt = DateTimeUtils.parseLocalDateTime("2026-10-29T19:30:00")
        assertEquals(2026, dt.year)
        assertEquals(10, dt.monthValue)
        assertEquals(29, dt.dayOfMonth)
        assertEquals(19, dt.hour)
        assertEquals(30, dt.minute)
    }

    @Test
    fun testFormatShowTime_12Hour() {
        val dt = LocalDateTime.of(2026, 10, 29, 19, 30)
        val formatted = DateTimeUtils.formatShowTime(dt)
        assertEquals("07:30 PM", formatted)

        val dtMorning = LocalDateTime.of(2026, 10, 29, 9, 15)
        assertEquals("09:15 AM", DateTimeUtils.formatShowTime(dtMorning))
    }

    @Test
    fun testFormatShowDate() {
        val dt = LocalDateTime.of(2026, 10, 29, 19, 30)
        val formatted = DateTimeUtils.formatShowDate("2026-10-29T19:30:00")
        assertEquals("29 Oct 2026", formatted)
    }

    @Test
    fun testTo12HourAndTo24HourConversion() {
        // Midnight 00:00 -> 12:00 AM
        val (h0, isAm0) = DateTimeUtils.to12Hour(0)
        assertEquals(12, h0)
        assertTrue(isAm0)
        assertEquals(0, DateTimeUtils.to24Hour(h0, isAm0))

        // Noon 12:00 -> 12:00 PM
        val (h12, isAm12) = DateTimeUtils.to12Hour(12)
        assertEquals(12, h12)
        assertFalse(isAm12)
        assertEquals(12, DateTimeUtils.to24Hour(h12, isAm12))

        // Evening 19:30 -> 7:30 PM
        val (h19, isAm19) = DateTimeUtils.to12Hour(19)
        assertEquals(7, h19)
        assertFalse(isAm19)
        assertEquals(19, DateTimeUtils.to24Hour(h19, isAm19))
    }

    @Test
    fun testFormatToIstDateTime_utcConversion() {
        // 2026-10-29T14:00:00Z in UTC is 19:30 in IST (+5:30)
        val istFormatted = DateTimeUtils.formatToIstDateTime("2026-10-29T14:00:00Z")
        assertTrue(istFormatted.contains("29 Oct 2026"))
        assertTrue(istFormatted.contains("07:30 PM"))
    }
}
