package ru.svetok.app.data.outage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface OutageCacheDao {
    @Query("SELECT * FROM cached_outages ORDER BY sortOrder ASC")
    fun observeOutages(): Flow<List<CachedOutageEntity>>

    @Query("SELECT * FROM outage_cache_meta WHERE id = :id")
    fun observeMeta(id: Int = CACHE_META_ID): Flow<OutageCacheMetaEntity?>

    @Query("SELECT * FROM cached_outages ORDER BY sortOrder ASC")
    suspend fun getOutages(): List<CachedOutageEntity>

    @Query("SELECT * FROM outage_cache_meta WHERE id = :id")
    suspend fun getMeta(id: Int = CACHE_META_ID): OutageCacheMetaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutages(outages: List<CachedOutageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMeta(meta: OutageCacheMetaEntity)

    @Query("DELETE FROM cached_outages")
    suspend fun clearOutages()

    @Transaction
    suspend fun replaceCache(meta: OutageCacheMetaEntity, outages: List<CachedOutageEntity>) {
        clearOutages()
        if (outages.isNotEmpty()) {
            insertOutages(outages)
        }
        upsertMeta(meta)
    }
}
