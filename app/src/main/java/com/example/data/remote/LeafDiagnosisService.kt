package com.example.data.remote

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "LeafDiagnosisService"

data class LeafDiagnosisResult(
    val plantName: String,
    val isHealthy: Boolean,
    val diseaseName: String,
    val confidencePercent: Int,
    val cause: String,
    val treatmentSteps: List<String>,
    val recommendedProducts: List<String>,
    val organicSolutions: List<String>,
    val preventionTips: List<String>
)

/**
 * Performs fast multimodal leaf disease detection using Firebase AI Logic
 * (Gemini 3.5 Flash-Lite primary for ultra-low latency, Gemini 3.8 Flash fallback)
 * with strict JSON responseSchema output in Bahasa Indonesia.
 */
class LeafDiagnosisService {

    private val diagnosisSchema = Schema.obj(
        properties = mapOf(
            "plantName" to Schema.string(
                description = "Nama tanaman singkat dalam Bahasa Indonesia (contoh: Padi, Cabai, Tomat, Jagung)"
            ),
            "isHealthy" to Schema.boolean(
                description = "true jika daun sehat, false jika kurang sehat atau ada penyakit/hama"
            ),
            "diseaseName" to Schema.string(
                description = "Nama penyakit dalam Bahasa Indonesia, atau 'Daun Sehat' jika sehat"
            ),
            "confidencePercent" to Schema.integer(
                description = "Tingkat keyakinan 0 sampai 100"
            ),
            "cause" to Schema.string(
                description = "Penjelasan singkat & jelas (1-2 kalimat) apakah daun sehat atau kurang sehat berdasarkan ciri visual serta penyebabnya"
            ),
            "treatmentSteps" to Schema.array(
                items = Schema.string(),
                description = "2 sampai 3 langkah singkat cara mengobati penyakit secara langsung"
            ),
            "recommendedProducts" to Schema.array(
                items = Schema.string(),
                description = "2 nama obat/fungisida/bakterisida/pestisida Indonesia yang cocok & dosis singkat (atau pupuk daun jika sehat)"
            ),
            "organicSolutions" to Schema.array(
                items = Schema.string(),
                description = "2 solusi organik/alami yang praktis"
            ),
            "preventionTips" to Schema.array(
                items = Schema.string(),
                description = "2 tips singkat pencegahan"
            )
        )
    )

    private val diagnosisConfig by lazy {
        generationConfig {
            responseMimeType = "application/json"
            responseSchema = diagnosisSchema
            temperature = 0.2f
            maxOutputTokens = 700
        }
    }

    private val primaryModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = diagnosisConfig
        )
    }

    private val fallbackModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = diagnosisConfig
        )
    }

    suspend fun analyzeLeaf(bitmap: Bitmap): LeafDiagnosisResult {
        val preparedBitmap = withContext(Dispatchers.Default) {
            ensureSoftwareArgb8888(scaleBitmapForUpload(bitmap, maxDimension = 640))
        }

        val promptText = """
            Anda ahli agronomi Indonesia untuk Agri Signal. Analisis foto daun ini secara cepat, padat, dan jelas dalam Bahasa Indonesia:
            1. Jelaskan singkat di 'cause' apakah daun sehat atau kurang sehat dan penyebabnya.
            2. Isi 'diseaseName' dengan nama penyakitnya (atau 'Daun Sehat').
            3. Beri 2-3 cara mengobati di 'treatmentSteps'.
            4. Sebutkan 2 nama obat pertanian Indonesia yang cocok di 'recommendedProducts' (atau pupuk daun jika sehat).
            5. Beri 2 solusi organik di 'organicSolutions' dan 2 tips pencegahan di 'preventionTips'.
        """.trimIndent()

        val attempts = listOf(
            primaryModel to 0L,
            fallbackModel to 300L
        )

        var lastError: Exception? = null
        for ((index, attempt) in attempts.withIndex()) {
            val (targetModel, delayMs) = attempt
            if (delayMs > 0L) delay(delayMs)

            try {
                val response = withTimeout(35_000L) {
                    targetModel.generateContent(
                        content {
                            image(preparedBitmap)
                            text(promptText)
                        }
                    )
                }
                val rawJson = response.text?.trim()
                    ?: throw IllegalStateException("AI tidak mengembalikan hasil analisis.")

                return parseDiagnosisResponse(rawJson)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Leaf analysis timed out on attempt ${index + 1}", e)
                lastError = IllegalStateException(
                    "Analisis AI melebihi batas waktu. Periksa koneksi internet dan coba lagi.",
                    e
                )
                if (index == attempts.lastIndex) throw lastError
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Leaf analysis failed on attempt ${index + 1}", e)
                lastError = e
                if (isAppCheckOrAuthError(e)) {
                    val rawDetail = e.localizedMessage?.takeIf { it.isNotBlank() } ?: "HTTP 403 / App Check"
                    throw IllegalStateException(
                        "Verifikasi Firebase App Check ditolak pada APK yang dibagikan langsung (sideloaded). " +
                            "Nonaktifkan paksaan (Unenforce) App Check untuk Firebase AI Logic di Firebase Console " +
                            "atau distribusikan melalui Google Play / daftarkan debug token perangkat. ($rawDetail)",
                        e
                    )
                }
                val canRetryOrFallback = index < attempts.lastIndex && isRateLimitOrTransient(e)
                if (!canRetryOrFallback) {
                    val detail = e.localizedMessage ?: "Gagal menghubungi layanan AI."
                    throw IllegalStateException("Deteksi AI gagal: $detail", e)
                }
            }
        }

        val detail = lastError?.localizedMessage ?: "Gagal menghubungi layanan AI."
        throw IllegalStateException("Deteksi AI gagal: $detail", lastError)
    }

    internal fun isAppCheckOrAuthError(error: Throwable): Boolean {
        var current: Throwable? = error
        while (current != null) {
            val msg = current.message.orEmpty().lowercase()
            if (
                msg.contains("403") ||
                msg.contains("app check") ||
                msg.contains("appcheck") ||
                msg.contains("attestation") ||
                msg.contains("firebaseappcheck") ||
                msg.contains("permission_denied") ||
                msg.contains("too many attempts") && msg.contains("app")
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }

    internal fun isRateLimitOrTransient(error: Throwable): Boolean {
        var current: Throwable? = error
        while (current != null) {
            val msg = current.message.orEmpty().lowercase()
            if (
                msg.contains("429") ||
                msg.contains("503") ||
                msg.contains("resource_exhausted") ||
                msg.contains("quota") ||
                msg.contains("rate") ||
                msg.contains("too many") ||
                msg.contains("unavailable") ||
                msg.contains("overloaded") ||
                msg.contains("try again")
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }

    internal fun parseDiagnosisResponse(rawJson: String): LeafDiagnosisResult {
        val json = JSONObject(rawJson.trim())
        val isHealthy = json.optBoolean("isHealthy", false)
        val rawDisease = json.optString(
            "diseaseName",
            if (isHealthy) "Daun Sehat" else "Gejala Terdeteksi"
        )
        return LeafDiagnosisResult(
            plantName = json.optString("plantName", "Tanaman Budidaya").ifBlank { "Tanaman Budidaya" },
            isHealthy = isHealthy,
            diseaseName = if (isHealthy && rawDisease.isBlank()) "Daun Sehat" else rawDisease,
            confidencePercent = json.optInt("confidencePercent", 75).coerceIn(1, 100),
            cause = json.optString("cause", "Analisis visual berdasarkan pola bercak dan warna daun."),
            treatmentSteps = json.optJSONArray("treatmentSteps").toStringList(),
            recommendedProducts = json.optJSONArray("recommendedProducts").toStringList(),
            organicSolutions = json.optJSONArray("organicSolutions").toStringList(),
            preventionTips = json.optJSONArray("preventionTips").toStringList()
        )
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return (0 until length())
            .mapNotNull { idx -> optString(idx)?.trim() }
            .filter { it.isNotEmpty() }
    }

    private fun scaleBitmapForUpload(source: Bitmap, maxDimension: Int): Bitmap {
        val width = source.width
        val height = source.height
        if (width <= maxDimension && height <= maxDimension) return source
        val ratio = minOf(maxDimension.toFloat() / width, maxDimension.toFloat() / height)
        val newWidth = (width * ratio).toInt().coerceAtLeast(1)
        val newHeight = (height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, newWidth, newHeight, true)
    }

    private fun ensureSoftwareArgb8888(source: Bitmap): Bitmap {
        return if (source.config == Bitmap.Config.ARGB_8888) {
            source
        } else {
            source.copy(Bitmap.Config.ARGB_8888, false)
        }
    }
}
