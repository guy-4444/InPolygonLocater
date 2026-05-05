package com.guy.polygonlocaterlibrary

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object PolygonLocater {

    private const val DEFAULT_ASSET_FILE_NAME = "countries.geojson"
    private const val UNKNOWN_CODE = "-99"

    @Volatile
    private var isLoaded: Boolean = false

    private val countries = mutableListOf<Country>()

    /**
     * Must be called once before using isInCountry() / whichCountry().
     *
     * Recommended place:
     * Application.onCreate()
     * or MainActivity.onCreate() before first usage.
     */
    fun init(
        context: Context,
        assetFileName: String = DEFAULT_ASSET_FILE_NAME
    ) {
        if (isLoaded) return

        synchronized(this) {
            if (isLoaded) return

            countries.clear()

            val jsonText = context.assets.open(assetFileName)
                .bufferedReader()
                .use { it.readText() }

            val root = JSONObject(jsonText)
            val features = root.getJSONArray("features")

            for (i in 0 until features.length()) {
                val feature = features.getJSONObject(i)

                val properties = feature.optJSONObject("properties") ?: continue
                val geometry = feature.optJSONObject("geometry") ?: continue

                val name = properties.optString("name", "")
                val alpha2 = properties.optString("ISO3166-1-Alpha-2", "")
                val alpha3 = properties.optString("ISO3166-1-Alpha-3", "")

                val polygons = parseGeometry(geometry)

                if (polygons.isNotEmpty()) {
                    countries.add(
                        Country(
                            name = name,
                            alpha2 = cleanCode(alpha2),
                            alpha3 = cleanCode(alpha3),
                            polygons = polygons
                        )
                    )
                }
            }

            isLoaded = true
        }
    }

    /**
     * Returns true if the given lat/lon is inside the requested country.
     *
     * countryCode may be:
     * - Alpha-2: "IL", "AU", "US"
     * - Alpha-3: "ISR", "AUS", "USA"
     */
    fun isInCountry(
        lat: Double,
        lon: Double,
        countryCode: String
    ): Boolean {
        ensureLoaded()

        val normalizedCode = countryCode.trim().uppercase(Locale.US)

        val country = countries.firstOrNull {
            it.alpha2 == normalizedCode || it.alpha3 == normalizedCode
        } ?: return false

        return country.contains(lat, lon)
    }

    /**
     * Returns Alpha-2 country code, for example:
     * "IL", "AU", "US".
     *
     * Returns null if the point is not inside any country.
     */
    fun whichCountry(
        lat: Double,
        lon: Double
    ): String? {
        ensureLoaded()

        return countries.firstOrNull { country ->
            country.contains(lat, lon)
        }?.alpha2
    }

    /**
     * Returns CountryInfo, for example:
     * CountryInfo(name="Israel", alpha2="IL", alpha3="ISR")
     * CountryInfo(name="Australia", alpha2="AU", alpha3="AUS")
     * CountryInfo(name="United States", alpha2="US", alpha3="USA")
     * Returns null if the point is not inside any country.
     * @see CountryInfo
     * @see Country
     *
     */
    fun whichCountryInfo(
        lat: Double,
        lon: Double
    ): CountryInfo? {
        ensureLoaded()

        val country = countries.firstOrNull { it.contains(lat, lon) }
            ?: return null

        return CountryInfo(
            name = country.name,
            alpha2 = country.alpha2,
            alpha3 = country.alpha3
        )
    }

    fun getSupportedCountries(): List<CountryInfo> {
        ensureLoaded()

        return countries.map {
            CountryInfo(
                name = it.name,
                alpha2 = it.alpha2,
                alpha3 = it.alpha3
            )
        }
    }

    private fun parseGeometry(geometry: JSONObject): List<GeoPolygon> {
        val type = geometry.optString("type")
        val coordinates = geometry.optJSONArray("coordinates") ?: return emptyList()

        return when (type) {
            "Polygon" -> {
                val polygon = parsePolygonCoordinates(coordinates)
                if (polygon != null) listOf(polygon) else emptyList()
            }

            "MultiPolygon" -> {
                val polygons = mutableListOf<GeoPolygon>()

                for (i in 0 until coordinates.length()) {
                    val polygonCoordinates = coordinates.getJSONArray(i)
                    val polygon = parsePolygonCoordinates(polygonCoordinates)

                    if (polygon != null) {
                        polygons.add(polygon)
                    }
                }

                polygons
            }

            else -> emptyList()
        }
    }

    /**
     * GeoJSON Polygon coordinates structure:
     *
     * [
     *   outerRing,
     *   holeRing1,
     *   holeRing2,
     *   ...
     * ]
     *
     * Each coordinate is:
     * [longitude, latitude]
     */
    private fun parsePolygonCoordinates(polygonCoordinates: JSONArray): GeoPolygon? {
        if (polygonCoordinates.length() == 0) return null

        val rings = mutableListOf<Ring>()

        for (i in 0 until polygonCoordinates.length()) {
            val ringArray = polygonCoordinates.getJSONArray(i)
            val ring = parseRing(ringArray)

            if (ring.points.size >= 3) {
                rings.add(ring)
            }
        }

        if (rings.isEmpty()) return null

        val outerRing = rings.first()
        val holes = rings.drop(1)

        return GeoPolygon(
            outerRing = outerRing,
            holes = holes
        )
    }

    /**
     * GeoJSON coordinate order is:
     * [longitude, latitude]
     *
     * Android / common location order is usually:
     * latitude, longitude
     */
    private fun parseRing(ringArray: JSONArray): Ring {
        val points = mutableListOf<GeoPoint>()

        for (i in 0 until ringArray.length()) {
            val coordinate = ringArray.getJSONArray(i)

            val lon = coordinate.getDouble(0)
            val lat = coordinate.getDouble(1)

            points.add(
                GeoPoint(
                    lat = lat,
                    lon = lon
                )
            )
        }

        return Ring(points)
    }

    private fun cleanCode(code: String): String {
        val trimmed = code.trim().uppercase(Locale.US)

        return if (trimmed.isEmpty() || trimmed == UNKNOWN_CODE) {
            ""
        } else {
            trimmed
        }
    }

    private fun ensureLoaded() {
        check(isLoaded) {
            "PolygonLocater is not initialized. Call PolygonLocater.init(context) before using it."
        }
    }

    data class CountryInfo(
        val name: String,
        val alpha2: String,
        val alpha3: String
    )

    private data class Country(
        val name: String,
        val alpha2: String,
        val alpha3: String,
        val polygons: List<GeoPolygon>
    ) {
        private val bounds = Bounds.fromPolygons(polygons)

        fun contains(lat: Double, lon: Double): Boolean {
            if (!bounds.contains(lat, lon)) return false

            return polygons.any { polygon ->
                polygon.contains(lat, lon)
            }
        }
    }

    private data class GeoPolygon(
        val outerRing: Ring,
        val holes: List<Ring>
    ) {
        private val bounds = Bounds.fromRing(outerRing)

        fun contains(lat: Double, lon: Double): Boolean {
            if (!bounds.contains(lat, lon)) return false

            if (!outerRing.contains(lat, lon)) return false

            val insideHole = holes.any { hole ->
                hole.contains(lat, lon)
            }

            return !insideHole
        }
    }

    private data class Ring(
        val points: List<GeoPoint>
    ) {
        val bounds = Bounds.fromPoints(points)

        fun contains(lat: Double, lon: Double): Boolean {
            if (!bounds.contains(lat, lon)) return false

            return isPointInRing(
                lat = lat,
                lon = lon,
                ring = points
            )
        }
    }

    private data class GeoPoint(
        val lat: Double,
        val lon: Double
    )

    private data class Bounds(
        val minLat: Double,
        val maxLat: Double,
        val minLon: Double,
        val maxLon: Double
    ) {
        fun contains(lat: Double, lon: Double): Boolean {
            return lat >= minLat &&
                    lat <= maxLat &&
                    lon >= minLon &&
                    lon <= maxLon
        }

        companion object {
            fun fromPoints(points: List<GeoPoint>): Bounds {
                var minLat = Double.POSITIVE_INFINITY
                var maxLat = Double.NEGATIVE_INFINITY
                var minLon = Double.POSITIVE_INFINITY
                var maxLon = Double.NEGATIVE_INFINITY

                for (point in points) {
                    minLat = min(minLat, point.lat)
                    maxLat = max(maxLat, point.lat)
                    minLon = min(minLon, point.lon)
                    maxLon = max(maxLon, point.lon)
                }

                return Bounds(
                    minLat = minLat,
                    maxLat = maxLat,
                    minLon = minLon,
                    maxLon = maxLon
                )
            }

            fun fromRing(ring: Ring): Bounds {
                return fromPoints(ring.points)
            }

            fun fromPolygons(polygons: List<GeoPolygon>): Bounds {
                var minLat = Double.POSITIVE_INFINITY
                var maxLat = Double.NEGATIVE_INFINITY
                var minLon = Double.POSITIVE_INFINITY
                var maxLon = Double.NEGATIVE_INFINITY

                for (polygon in polygons) {
                    val bounds = polygon.outerRing.bounds

                    minLat = min(minLat, bounds.minLat)
                    maxLat = max(maxLat, bounds.maxLat)
                    minLon = min(minLon, bounds.minLon)
                    maxLon = max(maxLon, bounds.maxLon)
                }

                return Bounds(
                    minLat = minLat,
                    maxLat = maxLat,
                    minLon = minLon,
                    maxLon = maxLon
                )
            }
        }
    }

    /**
     * Ray-casting algorithm.
     *
     * lat = y
     * lon = x
     */
    private fun isPointInRing(
        lat: Double,
        lon: Double,
        ring: List<GeoPoint>
    ): Boolean {
        if (ring.size < 3) return false

        var inside = false
        var j = ring.size - 1

        for (i in ring.indices) {
            val pointI = ring[i]
            val pointJ = ring[j]

            if (isPointOnSegment(
                    pointLat = lat,
                    pointLon = lon,
                    lat1 = pointI.lat,
                    lon1 = pointI.lon,
                    lat2 = pointJ.lat,
                    lon2 = pointJ.lon
                )
            ) {
                return true
            }

            val intersects = ((pointI.lat > lat) != (pointJ.lat > lat)) &&
                    (lon < (pointJ.lon - pointI.lon) *
                            (lat - pointI.lat) /
                            (pointJ.lat - pointI.lat) +
                            pointI.lon)

            if (intersects) {
                inside = !inside
            }

            j = i
        }

        return inside
    }

    /**
     * Border points count as inside.
     */
    private fun isPointOnSegment(
        pointLat: Double,
        pointLon: Double,
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Boolean {
        val epsilon = 1e-10

        val crossProduct =
            (pointLon - lon1) * (lat2 - lat1) -
                    (pointLat - lat1) * (lon2 - lon1)

        if (abs(crossProduct) > epsilon) return false

        return pointLat >= min(lat1, lat2) - epsilon &&
                pointLat <= max(lat1, lat2) + epsilon &&
                pointLon >= min(lon1, lon2) - epsilon &&
                pointLon <= max(lon1, lon2) + epsilon
    }
}