package com.cineplex.app.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTimeUtils {

    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
    private val TIME_12H_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)

    /**
     * Parses ISO-8601 or standard datetime strings without unintended timezone shifts.
     */
    fun parseLocalDateTime(raw: String?): LocalDateTime {
        if (raw.isNullOrBlank()) return LocalDateTime.now()
        return try {
            val sanitized = raw.trim().replace(" ", "T")
            val isoPart = if (sanitized.length >= 19) sanitized.substring(0, 19) else sanitized
            LocalDateTime.parse(isoPart, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        } catch (e: Exception) {
            try {
                LocalDateTime.parse(raw)
            } catch (e2: Exception) {
                LocalDateTime.now()
            }
        }
    }

    /**
     * Formats the show date header according to requirements:
     * - TODAY — 08 Oct 2026 (or இன்று — 08 Oct 2026)
     * - TOMORROW — 09 Oct 2026 (or நாளை — 09 Oct 2026)
     * - 10 OCT 2026
     */
    fun formatDateGroupHeader(
        date: LocalDate,
        todayLabel: String = "TODAY",
        tomorrowLabel: String = "TOMORROW",
        yesterdayLabel: String = "YESTERDAY"
    ): String {
        val today = LocalDate.now()
        val formattedDate = date.format(DATE_FORMATTER)
        return when (date) {
            today -> "$todayLabel — $formattedDate"
            today.plusDays(1) -> "$tomorrowLabel — $formattedDate"
            today.minusDays(1) -> "$yesterdayLabel — $formattedDate"
            else -> formattedDate.uppercase(Locale.ENGLISH)
        }
    }

    /**
     * Formats show time to 12-hour AM/PM format (e.g. "09:30 AM", "02:15 PM", "07:45 PM", "12:00 PM", "12:00 AM")
     */
    fun formatShowTime(dateTime: LocalDateTime): String {
        return dateTime.format(TIME_12H_FORMATTER).uppercase(Locale.ENGLISH)
    }

    fun formatShowTime(raw: String?): String {
        val dt = parseLocalDateTime(raw)
        return formatShowTime(dt)
    }

    /**
     * Formats show date only (e.g. "15 Oct 2026")
     */
    fun formatShowDate(raw: String?): String {
        val dt = parseLocalDateTime(raw)
        return dt.format(DATE_FORMATTER)
    }

    fun formatShowDate(date: LocalDate): String {
        return date.format(DATE_FORMATTER)
    }

    /**
     * Formats full show date and 12-hour AM/PM time (e.g. "15 Oct 2026, 07:45 PM")
     */
    fun formatShowDateTime(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val dt = parseLocalDateTime(raw)
        return "${dt.format(DATE_FORMATTER)}, ${formatShowTime(dt)}"
    }

    /**
     * Formats explicit 12-hour components: hour (1..12), minute (0..59), isAm
     */
    fun formatTime12H(hour12: Int, minute: Int, isAm: Boolean): String {
        val amPm = if (isAm) "AM" else "PM"
        return String.format(Locale.ENGLISH, "%02d:%02d %s", hour12, minute, amPm)
    }

    /**
     * Converts 12-hour clock (1..12) with AM/PM to 24-hour clock (0..23).
     * Correctly handles 12:00 AM -> 0 and 12:00 PM -> 12.
     */
    fun to24Hour(hour12: Int, isAm: Boolean): Int {
        val h = if (hour12 == 12) 0 else hour12
        return if (isAm) h else h + 12
    }

    /**
     * Converts 24-hour clock (0..23) to 12-hour clock (1..12) and isAm.
     * Correctly handles 0 -> (12, AM), 12 -> (12, PM).
     */
    fun to12Hour(hour24: Int): Pair<Int, Boolean> {
        return when {
            hour24 == 0 -> Pair(12, true)
            hour24 < 12 -> Pair(hour24, true)
            hour24 == 12 -> Pair(12, false)
            else -> Pair(hour24 - 12, false)
        }
    }

    /**
     * Builds standard ISO-8601 local datetime string without timezone shifts (e.g. "2026-10-15T19:45:00")
     */
    fun createIsoDateTime(date: LocalDate, hour12: Int, minute: Int, isAm: Boolean): String {
        val hour24 = to24Hour(hour12, isAm)
        val ldt = LocalDateTime.of(date, LocalTime.of(hour24, minute, 0))
        return ldt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }
}
