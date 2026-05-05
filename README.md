[![](https://jitpack.io/v/guy-4444/InPolygonLocater.svg)](https://jitpack.io/#guy-4444/InPolygonLocater)

# PolygonLocater

`PolygonLocater` is a lightweight Android/Kotlin utility library for answering geographic country-location questions using a local GeoJSON file.

It can answer questions such as:

- Is this latitude/longitude inside a specific country?
- Which country contains this latitude/longitude?
- What is the country name and ISO code for this location?
- Which countries are available in the local GeoJSON database?

The library works fully offline after the GeoJSON file is bundled inside the Android app assets.

---

## Features

- Works offline
- Uses a local `countries.geojson` file from the Android `assets` folder
- Supports GeoJSON `Polygon`
- Supports GeoJSON `MultiPolygon`
- Supports countries made of multiple islands or separated areas
- Supports polygons with holes
- Supports ISO Alpha-2 country codes, for example `IL`, `AU`, `US`
- Supports ISO Alpha-3 country codes, for example `ISR`, `AUS`, `USA`
- Returns full country information when needed
- Includes bounding-box optimization before expensive polygon checks
- Treats points on country borders as inside the country

---

## Implementation

#### To get a Git project into your build:

Step 1. Add the JitPack repository to your build file
```
	dependencyResolutionManagement {
		repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
		repositories {
			mavenCentral()
			maven { url 'https://jitpack.io' }
		}
	}
```

Step 2. Add the dependency
```
	dependencies {
	        implementation 'com.github.guy-4444:InPolygonLocater:1.00.03'
	}
```
## Current Package

The current package name is:

```kotlin
package com.guy.polygonlocaterlibrary
```

Import it using:

```kotlin
import com.guy.polygonlocaterlibrary.PolygonLocater
```

---

## Requirements

- Android project
- Kotlin
- A valid GeoJSON file in the app assets folder
- GeoJSON must contain a `FeatureCollection`
- Each feature should contain:
  - `properties.name`
  - `properties.ISO3166-1-Alpha-2`
  - `properties.ISO3166-1-Alpha-3`
  - `geometry.type`
  - `geometry.coordinates`

---

## GeoJSON File Location

Place the countries GeoJSON file here:

```text
app/src/main/assets/countries.geojson
```

The default asset file name expected by the library is:

```text
countries.geojson
```

Example project structure:

```text
app/
 └── src/
     └── main/
         ├── assets/
         │   └── countries.geojson
         ├── java/
         └── res/
```

---

## Expected GeoJSON Format

The library expects a GeoJSON `FeatureCollection`.

Example:

```json
{
  "type": "FeatureCollection",
  "name": "ne_10m_admin_0_countries",
  "features": [
    {
      "type": "Feature",
      "properties": {
        "name": "Israel",
        "ISO3166-1-Alpha-3": "ISR",
        "ISO3166-1-Alpha-2": "IL"
      },
      "geometry": {
        "type": "Polygon",
        "coordinates": [
          [
            [34.267, 31.220],
            [35.481, 31.220],
            [35.481, 33.335],
            [34.267, 33.335],
            [34.267, 31.220]
          ]
        ]
      }
    }
  ]
}
```

Important:

GeoJSON coordinates are written as:

```text
[longitude, latitude]
```

But Android location values are usually handled as:

```text
latitude, longitude
```

So when using the library, call it like this:

```kotlin
PolygonLocater.whichCountry(lat, lon)
```

Do not reverse the parameters.

---

## Initialization

You must initialize the library once before calling any country lookup function.

Recommended place:

- `Application.onCreate()`

Alternative:

- `MainActivity.onCreate()` before first usage

Example:

```kotlin
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        PolygonLocater.init(this)

        val lat = 31.96932735216634
        val lon = 34.790859063540395

        val isInIsrael = PolygonLocater.isInCountry(lat, lon, "IL")
        val countryCode = PolygonLocater.whichCountry(lat, lon)
        val countryInfo = PolygonLocater.whichCountryInfo(lat, lon)
    }
}
```

---

## Recommended Initialization in Application Class

For real apps, prefer initializing once in your custom `Application` class.

```kotlin
class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()

        PolygonLocater.init(this)
    }
}
```

Then register it in `AndroidManifest.xml`:

```xml
<application
    android:name=".MyApp"
    ... >
</application>
```

After that, you can call the library from anywhere after app startup:

```kotlin
val country = PolygonLocater.whichCountry(lat, lon)
```

---

## Basic Usage

### Check if a point is inside a country

```kotlin
val lat = 31.96932735216634
val lon = 34.790859063540395

val result = PolygonLocater.isInCountry(
    lat = lat,
    lon = lon,
    countryCode = "IL"
)

println(result)
```

Expected result:

```text
true
```

---

### Check using Alpha-3 country code

```kotlin
val result = PolygonLocater.isInCountry(
    lat = 31.96932735216634,
    lon = 34.790859063540395,
    countryCode = "ISR"
)
```

The library supports both:

```text
IL
ISR
```

---

### Find which country contains a point

```kotlin
val countryCode = PolygonLocater.whichCountry(
    lat = 31.96932735216634,
    lon = 34.790859063540395
)

println(countryCode)
```

Example result:

```text
IL
```

If the point is not inside any country in the GeoJSON file, the function returns:

```kotlin
null
```

---

### Get full country information

```kotlin
val countryInfo = PolygonLocater.whichCountryInfo(
    lat = 31.96932735216634,
    lon = 34.790859063540395
)

println(countryInfo?.name)
println(countryInfo?.alpha2)
println(countryInfo?.alpha3)
```

Example result:

```text
Israel
IL
ISR
```

---

### Get all supported countries

```kotlin
val countries = PolygonLocater.getSupportedCountries()

countries.forEach { country ->
    println("${country.name} - ${country.alpha2} - ${country.alpha3}")
}
```

---

## Public API

### `init`

```kotlin
fun init(
    context: Context,
    assetFileName: String = "countries.geojson"
)
```

Loads and parses the GeoJSON file from the Android assets folder.

Must be called before using:

- `isInCountry`
- `whichCountry`
- `whichCountryInfo`
- `getSupportedCountries`

Example:

```kotlin
PolygonLocater.init(context)
```

Custom asset file name:

```kotlin
PolygonLocater.init(context, "my_custom_countries.geojson")
```

---

### `isInCountry`

```kotlin
fun isInCountry(
    lat: Double,
    lon: Double,
    countryCode: String
): Boolean
```

Returns `true` if the location is inside the requested country.

Supported country code formats:

- Alpha-2: `IL`, `AU`, `US`
- Alpha-3: `ISR`, `AUS`, `USA`

Example:

```kotlin
val isInAustralia = PolygonLocater.isInCountry(
    lat = -33.8688,
    lon = 151.2093,
    countryCode = "AU"
)
```

---

### `whichCountry`

```kotlin
fun whichCountry(
    lat: Double,
    lon: Double
): String?
```

Returns the Alpha-2 country code of the country that contains the location.

Example:

```kotlin
val countryCode = PolygonLocater.whichCountry(
    lat = -33.8688,
    lon = 151.2093
)
```

Example result:

```text
AU
```

Returns `null` if no matching country is found.

---

### `whichCountryInfo`

```kotlin
fun whichCountryInfo(
    lat: Double,
    lon: Double
): CountryInfo?
```

Returns full country information.

Example:

```kotlin
val info = PolygonLocater.whichCountryInfo(
    lat = -33.8688,
    lon = 151.2093
)

println(info?.name)
println(info?.alpha2)
println(info?.alpha3)
```

Example result:

```text
Australia
AU
AUS
```

---

### `getSupportedCountries`

```kotlin
fun getSupportedCountries(): List<CountryInfo>
```

Returns all countries loaded from the GeoJSON file.

Example:

```kotlin
val supportedCountries = PolygonLocater.getSupportedCountries()
```

---

## CountryInfo

```kotlin
data class CountryInfo(
    val name: String,
    val alpha2: String,
    val alpha3: String
)
```

Example:

```kotlin
CountryInfo(
    name = "Israel",
    alpha2 = "IL",
    alpha3 = "ISR"
)
```

---

## How It Works

The library performs the following steps:

1. Loads `countries.geojson` from Android assets.
2. Parses the root `FeatureCollection`.
3. Reads every country feature.
4. Extracts:
   - country name
   - Alpha-2 code
   - Alpha-3 code
   - geometry
5. Parses `Polygon` and `MultiPolygon` geometries.
6. Converts GeoJSON coordinates from `[longitude, latitude]` into internal `lat/lon` objects.
7. Uses bounding boxes for fast rejection.
8. Uses a ray-casting point-in-polygon algorithm.
9. Checks polygon holes.
10. Returns the matching country.

---

## Supported Geometry Types

### Supported

```text
Polygon
MultiPolygon
```

### Not Supported

Currently, the library ignores other geometry types, such as:

```text
Point
MultiPoint
LineString
MultiLineString
GeometryCollection
```

For country borders, `Polygon` and `MultiPolygon` are usually enough.

---

## Polygon Holes

Some GeoJSON polygons contain holes.

A GeoJSON polygon may look like this:

```text
[
  outerRing,
  holeRing1,
  holeRing2
]
```

The library handles this correctly:

- First it checks if the point is inside the outer ring.
- Then it checks if the point is inside any hole.
- If the point is inside a hole, the final result is `false`.

---

## Border Behavior

Points located exactly on a country border are treated as inside the country.

This means:

```kotlin
PolygonLocater.isInCountry(borderLat, borderLon, "IL")
```

may return:

```text
true
```

This is intentional and usually preferable for app logic.

---

## Performance Notes

The library is simple and works well for normal app usage, but there are a few things to know.

### Initialization cost

Parsing a full high-resolution world GeoJSON file can take noticeable time.

For best UX, initialize once at app startup:

```kotlin
PolygonLocater.init(applicationContext)
```

Avoid calling `init()` repeatedly.

The current implementation prevents duplicate loading using an internal `isLoaded` flag.

### Lookup performance

For each lookup, the library checks countries one by one.

To improve performance, every country and polygon has a bounding box. This means the expensive polygon algorithm runs only for countries whose bounding box may contain the point.

This is good enough for many apps.

For heavy use cases, such as thousands of location checks per second, consider adding a spatial index.

---

## Thread Safety

Initialization is protected using `synchronized`.

This means calling `init()` from more than one place should not load the file multiple times.

However, recommended usage is still:

```kotlin
PolygonLocater.init(applicationContext)
```

once during app startup.

---

## Error Handling

If you call the library before initialization, it throws an error:

```text
PolygonLocater is not initialized. Call PolygonLocater.init(context) before using it.
```

Correct usage:

```kotlin
PolygonLocater.init(context)

val country = PolygonLocater.whichCountry(lat, lon)
```

Incorrect usage:

```kotlin
val country = PolygonLocater.whichCountry(lat, lon)
```

---

## Common Mistakes

### Mistake 1: Reversing latitude and longitude

Wrong:

```kotlin
PolygonLocater.whichCountry(lon, lat)
```

Correct:

```kotlin
PolygonLocater.whichCountry(lat, lon)
```

GeoJSON internally uses `[lon, lat]`, but the public API uses `lat, lon`.

---

### Mistake 2: Forgetting to initialize

Wrong:

```kotlin
val result = PolygonLocater.isInCountry(lat, lon, "IL")
```

Correct:

```kotlin
PolygonLocater.init(context)

val result = PolygonLocater.isInCountry(lat, lon, "IL")
```

---

### Mistake 3: Wrong asset location

Wrong:

```text
app/src/main/res/raw/countries.geojson
```

Correct:

```text
app/src/main/assets/countries.geojson
```

The current implementation loads the file using:

```kotlin
context.assets.open(assetFileName)
```

So the file must be inside the `assets` folder.

---

### Mistake 4: Missing ISO properties

The current parser expects these property names:

```json
"ISO3166-1-Alpha-2"
"ISO3166-1-Alpha-3"
```

If your GeoJSON uses different names, such as:

```json
"iso_a2"
"iso_a3"
```

you need to adjust the parser.

Current code:

```kotlin
val alpha2 = properties.optString("ISO3166-1-Alpha-2", "")
val alpha3 = properties.optString("ISO3166-1-Alpha-3", "")
```

---

## Example: Israel

```kotlin
PolygonLocater.init(context)

val lat = 31.96932735216634
val lon = 34.790859063540395

val isInIsrael = PolygonLocater.isInCountry(lat, lon, "IL")
val country = PolygonLocater.whichCountry(lat, lon)
val info = PolygonLocater.whichCountryInfo(lat, lon)

println(isInIsrael)
println(country)
println(info)
```

Expected result:

```text
true
IL
CountryInfo(name=Israel, alpha2=IL, alpha3=ISR)
```

---

## Example: Australia

```kotlin
PolygonLocater.init(context)

val lat = -33.8688
val lon = 151.2093

val isInAustralia = PolygonLocater.isInCountry(lat, lon, "AU")
val country = PolygonLocater.whichCountry(lat, lon)

println(isInAustralia)
println(country)
```

Expected result:

```text
true
AU
```

---

## Example: Unknown Ocean Point

```kotlin
PolygonLocater.init(context)

val lat = 0.0
val lon = -140.0

val country = PolygonLocater.whichCountry(lat, lon)

println(country)
```

Possible result:

```text
null
```

---

## Accuracy

Accuracy depends mainly on the GeoJSON file quality.

A high-resolution file, such as Natural Earth `10m`, gives better border and coastline accuracy but increases file size and parsing time.

A low-resolution file is faster and smaller but less accurate near borders and coastlines.

For serious location-sensitive applications, use a high-quality GeoJSON source and test carefully near borders.

---

## Limitations

- Does not handle disputed territories specially.
- Does not return multiple possible countries for border cases.
- Does not include maritime boundaries unless the GeoJSON file includes them.
- Does not use Android `Location` directly; it expects raw `lat` and `lon`.
- Does not currently support reloading a different GeoJSON file after initialization.
- Performance is fine for common usage, but not optimized for massive geospatial workloads.

---

## Suggested Future Improvements

Possible improvements for future versions:

- Add `reset()` or `reload()` for testing.
- Add support for returning Alpha-3 from `whichCountry`.
- Add support for returning country name directly.
- Add a spatial index for faster lookup.
- Add support for multiple matching countries on borders.
- Add unit tests with known coordinates.
- Add support for alternative GeoJSON property names.
- Add support for loading from `res/raw`.
- Add Java-friendly wrapper methods.
- Add coroutine/background initialization helper.

---

## Suggested Unit Tests

Recommended test points:

```kotlin
@Test
fun israelPointShouldReturnIL() {
    PolygonLocater.init(context)

    val result = PolygonLocater.whichCountry(
        lat = 31.96932735216634,
        lon = 34.790859063540395
    )

    assertEquals("IL", result)
}
```

```kotlin
@Test
fun sydneyShouldReturnAU() {
    PolygonLocater.init(context)

    val result = PolygonLocater.whichCountry(
        lat = -33.8688,
        lon = 151.2093
    )

    assertEquals("AU", result)
}
```

```kotlin
@Test
fun oceanPointShouldReturnNull() {
    PolygonLocater.init(context)

    val result = PolygonLocater.whichCountry(
        lat = 0.0,
        lon = -140.0
    )

    assertNull(result)
}
```

---

## License

Add your license here.

Example:

```text
MIT License
```

---

## Summary

`PolygonLocater` is a simple offline country-detection utility for Android.

Use it when you need to check whether a geographic coordinate is inside a country without calling an external API.

Basic flow:

```kotlin
PolygonLocater.init(context)

val isInside = PolygonLocater.isInCountry(lat, lon, "IL")
val countryCode = PolygonLocater.whichCountry(lat, lon)
val countryInfo = PolygonLocater.whichCountryInfo(lat, lon)
```
