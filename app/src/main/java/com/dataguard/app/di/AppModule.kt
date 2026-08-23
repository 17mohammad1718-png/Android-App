package com.dataguard.app.di

import android.content.Context
import androidx.room.Room
import com.dataguard.app.data.local.DataGuardDatabase
import com.dataguard.app.data.local.dao.AppDailyAggregateDao
import com.dataguard.app.data.local.dao.DataCapConfigDao
import com.dataguard.app.data.local.dao.UsageSnapshotDao
import com.dataguard.app.data.repository.DataCapRepositoryImpl
import com.dataguard.app.data.repository.DataUsageRepositoryImpl
import com.dataguard.app.data.repository.SnapshotRepositoryImpl
import com.dataguard.app.data.settings.SettingsRepositoryImpl
import com.dataguard.app.domain.repository.DataCapRepository
import com.dataguard.app.domain.repository.DataUsageRepository
import com.dataguard.app.domain.repository.SettingsRepository
import com.dataguard.app.domain.repository.SnapshotRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DataGuardDatabase =
        Room.databaseBuilder(context, DataGuardDatabase::class.java, "dataguard.db")
            // Destructive migration is acceptable for a local-first monitoring app:
            // all data can be reconstructed from NetworkStatsManager on next refresh.
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideUsageSnapshotDao(db: DataGuardDatabase): UsageSnapshotDao = db.usageSnapshotDao()

    @Provides
    fun provideDataCapConfigDao(db: DataGuardDatabase): DataCapConfigDao = db.dataCapConfigDao()

    @Provides
    fun provideAppDailyAggregateDao(db: DataGuardDatabase): AppDailyAggregateDao =
        db.appDailyAggregateDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindDataUsageRepository(impl: DataUsageRepositoryImpl): DataUsageRepository

    @Binds
    @Singleton
    abstract fun bindDataCapRepository(impl: DataCapRepositoryImpl): DataCapRepository

    @Binds
    @Singleton
    abstract fun bindSnapshotRepository(impl: SnapshotRepositoryImpl): SnapshotRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
