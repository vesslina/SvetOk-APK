package ru.svetok.app.data.geo

data class StreetFeature(
    val streetNorm: String,
    val displayName: String,
    val originalNames: List<String>,
    val segments: List<List<GeoPointDto>>,
)

data class GeoPointDto(
    val lat: Double,
    val lon: Double,
)
