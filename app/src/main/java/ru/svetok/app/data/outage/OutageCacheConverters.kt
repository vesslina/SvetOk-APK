package ru.svetok.app.data.outage

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class OutageCacheConverters {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @TypeConverter
    fun fromStringList(value: List<String>): String = json.encodeToString(value)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        value.takeIf(String::isNotBlank)?.let(json::decodeFromString) ?: emptyList()
}
