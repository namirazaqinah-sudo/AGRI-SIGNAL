package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class OpenMeteoResponse(
    @Json(name = "current") val current: CurrentWeatherDto?,
    @Json(name = "daily") val daily: DailyWeatherDto? = null
)

@JsonClass(generateAdapter = true)
data class CurrentWeatherDto(
    @Json(name = "temperature_2m") val temperature2m: Double?,
    @Json(name = "relative_humidity_2m") val relativeHumidity2m: Int?,
    @Json(name = "weather_code") val weatherCode: Int?,
    @Json(name = "wind_speed_10m") val windSpeed10m: Double?,
    @Json(name = "is_day") val isDay: Int?
)

@JsonClass(generateAdapter = true)
data class DailyWeatherDto(
    @Json(name = "time") val time: List<String>?,
    @Json(name = "weather_code") val weatherCode: List<Int>?,
    @Json(name = "temperature_2m_max") val tempMax: List<Double>?,
    @Json(name = "temperature_2m_min") val tempMin: List<Double>?
)

interface WeatherApiService {
    @GET("v1/forecast")
    suspend fun getCurrentWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m,is_day",
        @Query("daily") daily: String = "weather_code,temperature_2m_max,temperature_2m_min",
        @Query("forecast_days") forecastDays: Int = 3,
        @Query("timezone") timezone: String = "auto"
    ): OpenMeteoResponse

    companion object {
        fun create(): WeatherApiService {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()
            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
            return Retrofit.Builder()
                .baseUrl("https://api.open-meteo.com/")
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(WeatherApiService::class.java)
        }
    }
}

enum class WeatherVisualKind {
    SUNNY,
    PARTLY_CLOUDY,
    CLOUDY,
    FOGGY,
    RAIN,
    HEAVY_RAIN,
    STORM
}

data class WeatherDescription(
    val label: String,
    val advice: String,
    val visualKind: WeatherVisualKind
)

data class DailyForecast(
    val dayLabel: String,
    val weather: WeatherDescription,
    val maxTempCelsius: Double,
    val minTempCelsius: Double
)

object WeatherCodeMapper {
    fun map(code: Int, isDay: Boolean): WeatherDescription {
        return when (code) {
            0 -> WeatherDescription(
                label = if (isDay) "Cerah" else "Cerah Malam",
                advice = "Ideal untuk penyemprotan pagi atau sore hari.",
                visualKind = WeatherVisualKind.SUNNY
            )
            1, 2 -> WeatherDescription(
                label = "Cerah Berawan",
                advice = "Kondisi baik untuk pemantauan rutin tanaman.",
                visualKind = WeatherVisualKind.PARTLY_CLOUDY
            )
            3 -> WeatherDescription(
                label = "Berawan",
                advice = "Suhu teduh, perhatikan sirkulasi udara di sela daun.",
                visualKind = WeatherVisualKind.CLOUDY
            )
            45, 48 -> WeatherDescription(
                label = "Berkabut",
                advice = "Kelembapan tinggi, waspadai jamur embun tepung.",
                visualKind = WeatherVisualKind.FOGGY
            )
            51, 53, 55, 56, 57 -> WeatherDescription(
                label = "Gerimis",
                advice = "Tunda penyemprotan pestisida agar tidak terbilas.",
                visualKind = WeatherVisualKind.RAIN
            )
            61, 63, 80, 81 -> WeatherDescription(
                label = "Hujan Ringan - Sedang",
                advice = "Pastikan drainase bedengan lancar untuk cegah busuk akar.",
                visualKind = WeatherVisualKind.RAIN
            )
            65, 66, 67, 82 -> WeatherDescription(
                label = "Hujan Lebat",
                advice = "Periksa risiko genangan dan bercak daun pasca hujan.",
                visualKind = WeatherVisualKind.HEAVY_RAIN
            )
            95, 96, 99 -> WeatherDescription(
                label = "Hujan Disertai Petir",
                advice = "Utamakan keselamatan, hindari area terbuka sementara.",
                visualKind = WeatherVisualKind.STORM
            )
            else -> WeatherDescription(
                label = "Cuaca Berubah",
                advice = "Pantau terus kondisi kelembapan daun secara berkala.",
                visualKind = WeatherVisualKind.PARTLY_CLOUDY
            )
        }
    }
}
