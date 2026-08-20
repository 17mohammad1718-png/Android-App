package com.dataguard.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Raw, append-only snapshot of usage counters. Retained for auditing and to
 * enable delta-based computations in later phases (e.g. VPN capture).
 */
@Entity(tableName = "usage_snapshot")
data class UsageSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val appPackageName: String,
    val appName: String,
    val wifiBytesReceived: Long,
    val wifiBytesSent: Long,
    val mobileBytesReceived: Long,
    val mobileBytesSent: Long,
    val periodStart: Long,
    val periodEnd: Long,
)

/**
 * Singleton row (id is always 1) holding the user's data cap configuration.
 * [networkType] stores the enum name (WIFI / MOBILE / BOTH).
 */
@Entity(tableName = "data_cap_config")
data class DataCapConfigEntity(
    @PrimaryKey val id: Int = 1,
    val cycleStartDay: Int,
    val monthlyLimitBytes: Long,
    val alertThresholdPercent: Int,
    val networkType: String,
)

/**
 * Per-day, per-app totals. [date] is a LocalDate.toEpochDay() value, so it is
 * timezone-agnostic; labels are derived from it at read time.
 */
@Entity(tableName = "app_daily_aggregate", primaryKeys = ["date", "appPackageName"])
data class AppDailyAggregateEntity(
    val date: Long,
    val appPackageName: String,
    val totalWifiBytes: Long,
    val totalMobileBytes: Long,
)

/** Projection used by the history chart (daily totals across all apps). */
data class DailyTotalRow(
    val date: Long,
    val wifiBytes: Long,
    val mobileBytes: Long,
)
