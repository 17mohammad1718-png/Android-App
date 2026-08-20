package com.dataguard.app.domain.model

enum class NetworkType { WIFI, MOBILE, BOTH }

enum class UsagePeriod { DAY, WEEK, MONTH }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class DisplayUnit { AUTO, MB, GB }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val displayUnit: DisplayUnit = DisplayUnit.AUTO,
)

data class AppUsage(
    val uid: Int,
    val packageName: String,
    val appName: String,
    val wifiBytes: Long,
    val mobileBytes: Long,
) {
    val totalBytes: Long get() = wifiBytes + mobileBytes
}

data class TodayUsage(
    val wifiBytes: Long,
    val mobileBytes: Long,
) {
    val totalBytes: Long get() = wifiBytes + mobileBytes
}

data class HistoryPoint(
    val epochDay: Long,
    val label: String,
    val wifiBytes: Long,
    val mobileBytes: Long,
) {
    val totalBytes: Long get() = wifiBytes + mobileBytes
}

data class DataCap(
    val cycleStartDay: Int,
    val monthlyLimitBytes: Long,
    val alertThresholdPercent: Int,
    val networkType: NetworkType,
)

data class CapProgress(
    val cap: DataCap,
    val cycleStartMillis: Long,
    val cycleEndMillis: Long,
    val usedBytes: Long,
    val remainingBytes: Long,
    val percent: Float,
    val predictedEndMillis: Long?,
)
