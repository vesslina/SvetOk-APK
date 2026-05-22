package ru.svetok.app.data.outage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_outages")
data class CachedOutageEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val status: String,
    val reason: String,
    val timeLabel: String,
    val streetNorms: List<String>,
    val streetLabels: List<String>,
    val sortOrder: Int,
)

@Entity(tableName = "outage_cache_meta")
data class OutageCacheMetaEntity(
    @PrimaryKey
    val id: Int = CACHE_META_ID,
    val source: String,
    val infoMessage: String?,
    val updatedAtEpochMs: Long,
)

internal const val CACHE_META_ID = 1
