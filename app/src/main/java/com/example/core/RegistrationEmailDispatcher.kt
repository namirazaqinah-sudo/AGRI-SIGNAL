package com.example.core

import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.local.FarmerAccountEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val TAG = "RegistrationEmail"
private const val RESEND_EMAILS_ENDPOINT = "https://api.resend.com/emails"
private const val PLACEHOLDER_KEY = "YOUR_RESEND_API_KEY"

/**
 * Automatically sends newly registered farmer data and the generated Excel (.xlsx)
 * workbook in the background to namirazaqinah@gmail.com via Resend HTTPS API.
 */
object RegistrationEmailDispatcher {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    suspend fun sendRegistrationExcel(
        excelFile: File,
        latestFarmer: FarmerAccountEntity,
        totalRegistered: Int
    ): Boolean = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.RESEND_API_KEY.trim()
        if (apiKey.isEmpty() || apiKey == PLACEHOLDER_KEY) {
            Log.w(
                TAG,
                "RESEND_API_KEY belum dikonfigurasi di panel Secrets; file Excel disimpan lokal tanpa pengiriman email otomatis."
            )
            return@withContext false
        }

        runCatching {
            val xlsxBytes = excelFile.readBytes()
            val payload = buildResendPayloadJson(
                xlsxBytes = xlsxBytes,
                latestFarmer = latestFarmer,
                totalRegistered = totalRegistered
            )
            val requestBody = payload.toString()
                .toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url(RESEND_EMAILS_ENDPOINT)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    Log.d(TAG, "Data pendaftaran Excel otomatis terkirim ke ${FarmerExcelExporter.TARGET_EMAIL}: $bodyStr")
                    true
                } else {
                    Log.e(TAG, "Gagal mengirim email otomatis (HTTP ${response.code}): $bodyStr")
                    false
                }
            }
        }.onFailure { err ->
            Log.e(TAG, "Kesalahan jaringan saat mengirim data pendaftaran ke ${FarmerExcelExporter.TARGET_EMAIL}", err)
        }.getOrDefault(false)
    }

    internal fun buildResendPayloadJson(
        xlsxBytes: ByteArray,
        latestFarmer: FarmerAccountEntity,
        totalRegistered: Int
    ): JSONObject {
        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("id", "ID"))
        val dateText = formatter.format(Date(latestFarmer.createdAtMillis))
        val encodedXlsx = Base64.encodeToString(xlsxBytes, Base64.NO_WRAP)

        val summaryText = buildString {
            appendLine("Pendaftaran Baru Aplikasi AGRI SIGNAL")
            appendLine()
            appendLine("Nama Lengkap   : ${latestFarmer.fullName}")
            appendLine("Nomor Handphone: ${latestFarmer.phoneNumber}")
            appendLine("Waktu Daftar   : $dateText")
            appendLine("Total Pendaftar: $totalRegistered akun")
            appendLine()
            append("Terlampir file Excel (.xlsx) berisi data pendaftar AGRI SIGNAL.")
        }

        val attachment = JSONObject().apply {
            put("filename", "data_pendaftar_agri_signal.xlsx")
            put("content", encodedXlsx)
        }

        return JSONObject().apply {
            put("from", "AGRI SIGNAL <onboarding@resend.dev>")
            put("to", JSONArray().put(FarmerExcelExporter.TARGET_EMAIL))
            put("subject", "Data Pendaftar Baru AGRI SIGNAL - ${latestFarmer.fullName}")
            put("text", summaryText)
            put("attachments", JSONArray().put(attachment))
        }
    }
}
