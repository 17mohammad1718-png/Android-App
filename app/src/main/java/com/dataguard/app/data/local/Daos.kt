package com.dataguard.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.dataguard.app.data.local.entity.AppDailyAggregateEntity
import com.dataguard.app.data.local.entity.DataCapConfigEntity
import com.dataguard.app.data.local.entity.DailyTotalRow
import com.dataguard.app.data.local.entity.UsageSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageSnapshotDao {

    /**
     * Insert raw audit snapshots. Uses IGNORE because this table is append-only
     * and the worker may occasionally re-run for the same interval; duplicates
     * are silently skipped rather than causing a failure.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(rows: List<UsageSnapshotEntity>)

    @Query("SELECT MAX(timestamp) FROM usage_snapshot")
    suspend fun latestTimestamp(): Long?

    /** Prunes raw audit snapshots; returns the number of deleted rows. */
    @Query("DELETE FROM usage_snapshot WHERE timestamp < :cutoffMillis")
    suspend fun deleteOlderThan(cutoffMillis: Long): Int
}

@Dao
interface DataCapConfigDao {

    @Query("SELECT * FROM data_cap_config WHERE id = 1")
    fun observe(): Flow<DataCapConfigEntity?>

    @Query("SELECT * FROM data_cap_config WHERE id = 1")
    suspend fun get(): DataCapConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(config: DataCapConfigEntity)
}

@Dao
interface AppDailyAggregateDao {

    @Upsert
    suspend fun upsertAll(rows: List<AppDailyAggregateEntity>)

    @Query(
        """
        SELECT date AS date,
               SUM(totalWifiBytes) AS wifiBytes,
               SUM(totalMobileBytes) AS mobileBytes
        FROM app_daily_aggregate
        WHERE date >= :fromEpochDay
        GROUP BY date
        ORDER BY date ASC
        """,
    )
    suspend fun dailyTotals(fromEpochDay: Long): List<DailyTotalRow>
}
