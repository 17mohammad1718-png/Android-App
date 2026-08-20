package com.dataguard.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dataguard.app.data.local.dao.AppDailyAggregateDao
import com.dataguard.app.data.local.dao.DataCapConfigDao
import com.dataguard.app.data.local.dao.UsageSnapshotDao
import com.dataguard.app.data.local.entity.AppDailyAggregateEntity
import com.dataguard.app.data.local.entity.DataCapConfigEntity
import com.dataguard.app.data.local.entity.UsageSnapshotEntity

@Database(
    entities = [
        UsageSnapshotEntity::class,
        DataCapConfigEntity::class,
        AppDailyAggregateEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class DataGuardDatabase : RoomDatabase() {
    abstract fun usageSnapshotDao(): UsageSnapshotDao
    abstract fun dataCapConfigDao(): DataCapConfigDao
    abstract fun appDailyAggregateDao(): AppDailyAggregateDao
}
