package com.example.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.util.Log
import com.example.data.remote.DailyForecast
import com.example.data.remote.NominatimApiService
import com.example.data.remote.PlaceHierarchy
import com.example.data.remote.WeatherApiService
import com.example.data.remote.WeatherCodeMapper
import com.example.data.remote.WeatherDescription
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

private const val TAG = "EnvironmentRepository"

data class EnvironmentSnapshot(
    val latitude: Double,
    val longitude: Double,
    val temperatureCelsius: Double,
    val humidityPercent: Int,
    val windSpeedKmh: Double,
    val weather: WeatherDescription,
    val place: PlaceHierarchy,
    val dailyForecasts: List<DailyForecast> = emptyList(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

class EnvironmentRepository(
    private val context: Context,
    private val weatherApi: WeatherApiService = WeatherApiService.create(),
    private val nominatimApi: NominatimApiService = NominatimApiService.create()
) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentEnvironment(): EnvironmentSnapshot {
        val location = getCurrentOrLastLocation()
            ?: throw IllegalStateException("Sinyal GPS belum ditemukan. Pastikan layanan lokasi (GPS) perangkat aktif lalu tekan Perbarui.")

        val lat = location.latitude
        val lon = location.longitude

        val weatherResp = withContext(Dispatchers.IO) {
            weatherApi.getCurrentWeather(latitude = lat, longitude = lon)
        }
        val current = weatherResp.current
            ?: throw IllegalStateException("Data cuaca tidak tersedia dari server.")

        val place = resolvePlaceHierarchy(lat, lon)
        val weatherDesc = WeatherCodeMapper.map(
            code = current.weatherCode ?: 1,
            isDay = (current.isDay ?: 1) == 1
        )

        val dayLabels = listOf("Hari Ini", "Besok", "Lusa")
        val daily = weatherResp.daily
        val forecasts = if (daily != null && !daily.weatherCode.isNullOrEmpty()) {
            dayLabels.indices.mapNotNull { idx ->
                val code = daily.weatherCode.getOrNull(idx) ?: return@mapNotNull null
                val maxT = daily.tempMax?.getOrNull(idx) ?: current.temperature2m ?: 28.0
                val minT = daily.tempMin?.getOrNull(idx) ?: (maxT - 5.0)
                DailyForecast(
                    dayLabel = dayLabels[idx],
                    weather = WeatherCodeMapper.map(code = code, isDay = true),
                    maxTempCelsius = maxT,
                    minTempCelsius = minT
                )
            }
        } else {
            emptyList()
        }

        return EnvironmentSnapshot(
            latitude = lat,
            longitude = lon,
            temperatureCelsius = current.temperature2m ?: 0.0,
            humidityPercent = current.relativeHumidity2m ?: 0,
            windSpeedKmh = current.windSpeed10m ?: 0.0,
            weather = weatherDesc,
            place = place,
            dailyForecasts = forecasts
        )
    }

    @SuppressLint("MissingPermission")
    private suspend fun getCurrentOrLastLocation(): Location? {
        val current = suspendCancellableCoroutine<Location?> { cont ->
            val cts = CancellationTokenSource()
            cont.invokeOnCancellation { cts.cancel() }
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                .addOnSuccessListener { loc -> cont.resume(loc) }
                .addOnFailureListener { e ->
                    Log.w(TAG, "getCurrentLocation failed: ${e.message}")
                    cont.resume(null)
                }
        }
        if (current != null) return current

        return suspendCancellableCoroutine { cont ->
            fusedLocationClient.lastLocation
                .addOnSuccessListener { loc -> cont.resume(loc) }
                .addOnFailureListener { e ->
                    Log.w(TAG, "lastLocation failed: ${e.message}")
                    cont.resume(null)
                }
        }
    }

    private suspend fun resolvePlaceHierarchy(lat: Double, lon: Double): PlaceHierarchy {
        try {
            val response = withContext(Dispatchers.IO) {
                nominatimApi.reverseGeocode(latitude = lat, longitude = lon)
            }
            val addr = response.address
            if (addr != null) {
                val desa = listOfNotNull(addr.village, addr.suburb, addr.neighbourhood, addr.hamlet)
                    .firstOrNull { it.isNotBlank() }.orEmpty()
                val kecamatan = listOfNotNull(addr.cityDistrict, addr.subdistrict, addr.town, addr.municipality)
                    .firstOrNull { it.isNotBlank() }.orEmpty()
                val kabupaten = listOfNotNull(addr.regency, addr.county, addr.city)
                    .firstOrNull { it.isNotBlank() }.orEmpty()
                val provinsi = addr.state.orEmpty()
                if (desa.isNotBlank() || kecamatan.isNotBlank() || kabupaten.isNotBlank() || provinsi.isNotBlank()) {
                    return PlaceHierarchy(
                        desaKelurahan = desa,
                        kecamatan = kecamatan,
                        kabupatenKota = kabupaten,
                        provinsi = provinsi
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Nominatim reverse geocode fallback to Android Geocoder: ${e.message}")
        }

        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale("id", "ID"))
                @Suppress("DEPRECATION")
                val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine { cont ->
                        geocoder.getFromLocation(lat, lon, 1) { list ->
                            cont.resume(list)
                        }
                    }
                } else {
                    geocoder.getFromLocation(lat, lon, 1).orEmpty()
                }
                val first = addresses.firstOrNull()
                if (first != null) {
                    PlaceHierarchy(
                        desaKelurahan = first.subLocality.orEmpty(),
                        kecamatan = first.locality.orEmpty(),
                        kabupatenKota = first.subAdminArea.orEmpty(),
                        provinsi = first.adminArea.orEmpty()
                    )
                } else {
                    PlaceHierarchy("", "", "Koordinat %.4f, %.4f".format(lat, lon), "")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Android Geocoder failed: ${e.message}")
                PlaceHierarchy("", "", "Koordinat %.4f, %.4f".format(lat, lon), "")
            }
        }
    }
}
