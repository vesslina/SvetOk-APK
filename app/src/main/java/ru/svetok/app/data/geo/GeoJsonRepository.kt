package ru.svetok.app.data.geo

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class GeoJsonRepository(
    private val context: Context,
) {
    fun loadStreetFeatures(): List<StreetFeature> {
        val jsonString = context.assets.open("streets.geojson").bufferedReader().use { it.readText() }
        val root = JSONObject(jsonString)
        val features = root.getJSONArray("features")

        return buildList(features.length()) {
            for (index in 0 until features.length()) {
                add(parseFeature(features.getJSONObject(index)))
            }
        }
    }

    private fun parseFeature(featureObject: JSONObject): StreetFeature {
        val properties = featureObject.getJSONObject("properties")
        val geometry = featureObject.getJSONObject("geometry")
        val coordinates = geometry.getJSONArray("coordinates")

        return StreetFeature(
            streetNorm = properties.getString("street_norm"),
            displayName = properties.getString("display_name"),
            originalNames = properties.optJSONArray("original_names").toStringList(),
            segments = coordinates.toSegments(),
        )
    }

    private fun JSONArray.toSegments(): List<List<GeoPointDto>> = buildList(length()) {
        for (segmentIndex in 0 until length()) {
            val segment = getJSONArray(segmentIndex)
            add(segment.toPoints())
        }
    }

    private fun JSONArray.toPoints(): List<GeoPointDto> = buildList(length()) {
        for (pointIndex in 0 until length()) {
            val point = getJSONArray(pointIndex)
            add(
                GeoPointDto(
                    lon = point.getDouble(0),
                    lat = point.getDouble(1),
                )
            )
        }
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()

        return buildList(length()) {
            for (index in 0 until length()) {
                add(getString(index))
            }
        }
    }
}
