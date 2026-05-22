package ru.svetok.app.data.outage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [CachedOutageEntity::class, OutageCacheMetaEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(OutageCacheConverters::class)
abstract class OutageDatabase : RoomDatabase() {
    abstract fun outageCacheDao(): OutageCacheDao
}

fun createOutageDatabase(context: Context): OutageDatabase =
    Room.databaseBuilder(
        context,
        OutageDatabase::class.java,
        "svetok-outages.db",
    ).fallbackToDestructiveMigration(false).build()
