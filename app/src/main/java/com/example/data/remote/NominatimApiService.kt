package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class NominatimResponse(
    @Json(name = "display_name") val displayName: String?,
    @Json(name = "address") val address: NominatimAddressDto?
)

@JsonClass(generateAdapter = true)
data class NominatimAddressDto(
    @Json(name = "village") val village: String?,
    @Json(name = "suburb") val suburb: String?,
    @Json(name = "neighbourhood") val neighbourhood: String?,
    @Json(name = "hamlet") val hamlet: String?,
    @Json(name = "city_district") val cityDistrict: String?,
    @Json(name = "subdistrict") val subdistrict: String?,
    @Json(name = "town") val town: String?,
    @Json(name = "municipality") val municipality: String?,
    @Json(name = "county") val county: String?,
    @Json(name = "city") val city: String?,
    @Json(name = "regency") val regency: String?,
    @Json(name = "state") val state: String?,
    @Json(name = "country") val country: String?
)

interface NominatimApiService {
    @GET("reverse")
    suspend fun reverseGeocode(
        @Query("format") format: String = "jsonv2",
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("addressdetails") addressDetails: Int = 1,
        @Query("accept-language") acceptLanguage: String = "id"
    ): NominatimResponse

    companion object {
        fun create(): NominatimApiService {
            val userAgentInterceptor = Interceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("User-Agent", "AgriSignalAndroid/1.0 (https://agrisignal.app)")
                    .build()
                chain.proceed(req)
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(userAgentInterceptor)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()
            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
            return Retrofit.Builder()
                .baseUrl("https://nominatim.openstreetmap.org/")
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(NominatimApiService::class.java)
        }
    }
}

data class PlaceHierarchy(
    val desaKelurahan: String,
    val kecamatan: String,
    val kabupatenKota: String,
    val provinsi: String
) {
    fun shortLabel(): String {
        val parts = listOf(desaKelurahan, kabupatenKota).filter { it.isNotBlank() }
        return if (parts.isNotEmpty()) parts.joinToString(", ") else "Lokasi Terdeteksi"
    }

    fun fullLabel(): String {
        val parts = listOf(desaKelurahan, kecamatan, kabupatenKota, provinsi).filter { it.isNotBlank() }
        return if (parts.isNotEmpty()) parts.joinToString(", ") else "Koordinat GPS Aktif"
    }
}
