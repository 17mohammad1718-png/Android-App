package com.dataguard.app.data.repository

import com.dataguard.app.data.local.dao.DataCapConfigDao
import com.dataguard.app.data.local.entity.DataCapConfigEntity
import com.dataguard.app.domain.model.DataCap
import com.dataguard.app.domain.model.NetworkType
import com.dataguard.app.domain.repository.DataCapRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataCapRepositoryImpl @Inject constructor(
    private val dao: DataCapConfigDao,
) : DataCapRepository {

    override fun observeCap(): Flow<DataCap?> = dao.observe().map { it?.toDomain() }

    override suspend fun getCap(): DataCap? = dao.get()?.toDomain()

    override suspend fun saveCap(cap: DataCap) {
        dao.upsert(
            DataCapConfigEntity(
                id = 1,
                cycleStartDay = cap.cycleStartDay,
                monthlyLimitBytes = cap.monthlyLimitBytes,
                alertThresholdPercent = cap.alertThresholdPercent,
                networkType = cap.networkType.name,
            ),
        )
    }

    private fun DataCapConfigEntity.toDomain(): DataCap = DataCap(
        cycleStartDay = cycleStartDay,
        monthlyLimitBytes = monthlyLimitBytes,
        alertThresholdPercent = alertThresholdPercent,
        networkType = runCatching { NetworkType.valueOf(networkType) }
            .getOrDefault(NetworkType.MOBILE),
    )
}
