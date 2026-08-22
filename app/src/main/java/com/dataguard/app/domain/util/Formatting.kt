package com.dataguard.app.domain.util

import com.dataguard.app.domain.model.DisplayUnit
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object ByteFormatter {

    /**
     * Formats bytes as a compact human-readable string, localizing digits for Persian.
     * When [unit] is not [DisplayUnit.AUTO], the value is always expressed in that unit
     * (e.g. 3072 MB instead of 3 GB), matching the user's display-unit preference.
     */
    fun format(bytes: Long, unit: DisplayUnit = DisplayUnit.AUTO): String {
        val locale = Locale.getDefault()
        val kb = 1024.0
        val mb = kb * 1024
        val gb = mb * 1024

        val value: Double
        val unitLabel: String
        when (unit) {
            DisplayUnit.AUTO -> when {
                bytes >= gb -> {
                    value = bytes / gb
                    unitLabel = "GB"
                }
                bytes >= mb -> {
                    value = bytes / mb
                    unitLabel = "MB"
                }
                bytes >= kb -> {
                    value = bytes / kb
                    unitLabel = "KB"
                }
                else -> {
                    value = bytes.toDouble()
                    unitLabel = "B"
                }
            }
            DisplayUnit.MB -> {
                value = bytes / mb
                unitLabel = "MB"
            }
            DisplayUnit.GB -> {
                value = bytes / gb
                unitLabel = "GB"
            }
        }

        val number = if (value >= 100) {
            String.format(Locale.US, "%.0f", value)
        } else {
            String.format(Locale.US, "%.1f", value)
        }
        return localizeDigits(number, locale) + " " + localizeUnit(unitLabel, locale)
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
