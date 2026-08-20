package com.dataguard.app.domain.util

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object ByteFormatter {

    /** Formats bytes as a compact human-readable string, localizing digits for Persian. */
    fun format(bytes: Long): String {
        val locale = Locale.getDefault()
        val kb = 1024.0
        val mb = kb * 1024
        val gb = mb * 1024

        val value: Double
        val unit: String
        when {
            bytes >= gb -> {
                value = bytes / gb
                unit = "GB"
            }
            bytes >= mb -> {
                value = bytes / mb
                unit = "MB"
            }
            bytes >= kb -> {
                value = bytes / kb
                unit = "KB"
            }
            else -> {
                value = bytes.toDouble()
                unit = "B"
            }
        }

        val number = if (value >= 100) {
            String.format(Locale.US, "%.0f", value)
        } else {
            String.format(Locale.US, "%.1f", value)
        }
        return localizeDigits(number, locale) + " " + localizeUnit(unit, locale)
    }

    private fun localizeDigits(s: String, locale: Locale): String {
        if (locale.language != "fa") return s
        val fa = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        return buildString {
            for (c in s) {
                append(
                    when {
                        c in '0'..'9' -> fa[c - '0']
                        c == '.' -> '٫'
                        else -> c
                    },
                )
            }
        }
    }

    private fun localizeUnit(unit: String, locale: Locale): String {
        if (locale.language != "fa") return unit
        return when (unit) {
            "GB" -> "گیگابایت"
            "MB" -> "مگابایت"
            "KB" -> "کیلوبایت"
            else -> "بایت"
        }
    }
}

object DateUtils {

    fun epochDay(date: LocalDate = LocalDate.now()): Long = date.toEpochDay()

    fun startOfDayMillis(
        date: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long = date.atStartOfDay(zone).toInstant().toEpochMilli()

    fun endOfDayMillis(
        date: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

    /** Short label for a stored epoch-day, localized (e.g. "Aug 20" / "۲۰ اوت"). */
    fun label(epochDay: Long): String =
        LocalDate.ofEpochDay(epochDay).format(
            DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()),
        )
}
