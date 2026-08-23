package com.dataguard.app.data.networkstats

import android.app.AppOpsManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Process
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mutable accumulation bucket for a single UID.
 * Keeps Wi-Fi and mobile rx/tx separate so raw snapshots stay faithful.
 */
data class AppUsageRaw(
    val uid: Int,
    val wifiRx: Long = 0,
    val wifiTx: Long = 0,
    val mobileRx: Long = 0,
    val mobileTx: Long = 0,
) {
    val wifiBytes: Long get() = wifiRx + wifiTx
    val mobileBytes: Long get() = mobileRx + mobileTx
    val totalBytes: Long get() = wifiBytes + mobileBytes

    fun withWifi(rx: Long, tx: Long) = copy(wifiRx = wifiRx + rx, wifiTx = wifiTx + tx)
    fun withMobile(rx: Long, tx: Long) = copy(mobileRx = mobileRx + rx, mobileTx = mobileTx + tx)
}

/**
 * Thin wrapper over [NetworkStatsManager] (primary source) plus the
 * "usage access" permission check. [android.net.TrafficStats] is intentionally
 * NOT used for history — its counters reset on reboot and lack network-type
 * history. It can be added later as a boot-time fallback if needed.
 */
@Singleton
class NetworkStatsDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val TAG = "NetworkStatsDataSource"
    }

    private val statsManager: NetworkStatsManager =
        context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager

    private val packageManager: PackageManager = context.packageManager

    /**
     * Cache for UID → (packageName, displayLabel) resolution.
     * Cleared only on process death — UIDs are stable within a single boot.
     */
    private val resolveCache = mutableMapOf<Int, Pair<String, String>>()

    /** Whether the user has granted the special PACKAGE_USAGE_STATS app-op. */
    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Total (rx + tx) device-wide bytes for a network type over [start, end). */
    fun queryDeviceTotal(networkType: Int, start: Long, end: Long): Long {
        if (!hasUsageAccess()) return 0L
        return try {
            // querySummaryForDevice returns a single aggregate Bucket — no iteration needed.
            val bucket = statsManager.querySummaryForDevice(networkType, null, start, end)
            bucket.rxBytes + bucket.txBytes
        } catch (e: Exception) {
            Log.w(TAG, "queryDeviceTotal failed for type=$networkType", e)
            0L
        }
    }

    fun queryWifiTotal(start: Long, end: Long): Long =
        queryDeviceTotal(ConnectivityManager.TYPE_WIFI, start, end)

    fun queryMobileTotal(start: Long, end: Long): Long =
        queryDeviceTotal(ConnectivityManager.TYPE_MOBILE, start, end)

    /** Per-UID Wi-Fi + mobile usage over [start, end), combined and filtered to non-zero. */
    fun queryAppUsage(start: Long, end: Long): List<AppUsageRaw> {
        if (!hasUsageAccess()) return emptyList()
        val map = mutableMapOf<Int, AppUsageRaw>()
        accumulate(map, ConnectivityManager.TYPE_WIFI, start, end, isWifi = true)
        accumulate(map, ConnectivityManager.TYPE_MOBILE, start, end, isWifi = false)
        return map.values.filter { it.totalBytes > 0 }
    }

    private fun accumulate(
        map: MutableMap<Int, AppUsageRaw>,
        networkType: Int,
        start: Long,
        end: Long,
        isWifi: Boolean,
    ) {
        try {
            // Use .use {} to properly close NetworkStats and avoid resource leaks.
            statsManager.queryDetails(networkType, null, start, end).use { stats ->
                val bucket = NetworkStats.Bucket()
                while (stats.hasNextBucket()) {
                    stats.getNextBucket(bucket)
                    val existing = map[bucket.uid] ?: AppUsageRaw(bucket.uid)
                    map[bucket.uid] = if (isWifi) {
                        existing.withWifi(bucket.rxBytes, bucket.txBytes)
                    } else {
                        existing.withMobile(bucket.rxBytes, bucket.txBytes)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "accumulate failed for type=$networkType", e)
        }
    }

    /** Resolve a UID to its (packageName, displayLabel), with caching. */
    fun resolveApp(uid: Int): Pair<String, String> {
        resolveCache[uid]?.let { return it }
        val result = resolveAppUncached(uid)
        resolveCache[uid] = result
        return result
    }

    private fun resolveAppUncached(uid: Int): Pair<String, String> {
        val pkg = packageManager.getPackagesForUid(uid)?.firstOrNull()
        if (pkg == null) return ("uid_$uid") to ("UID $uid")
        val label = try {
            val info = packageManager.getApplicationInfo(pkg, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            Log.d(TAG, "getApplicationInfo failed for $pkg", e)
            pkg
        }
        return pkg to label
    }
}
