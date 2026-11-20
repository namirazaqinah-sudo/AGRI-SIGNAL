package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import com.example.data.local.ScanRecordDao
import com.example.data.local.ScanRecordEntity
import com.example.data.remote.LeafDiagnosisService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class LeafScanRepository(
    private val context: Context,
    private val scanRecordDao: ScanRecordDao,
    private val diagnosisService: LeafDiagnosisService = LeafDiagnosisService()
) {
    fun observeHistoryForUser(userPhone: String): Flow<List<ScanRecordEntity>> =
        scanRecordDao.observeRecordsByUser(userPhone)

    suspend fun analyzeAndSaveLeaf(
        userPhone: String,
        imageUri: Uri,
        locationSummary: String,
        temperatureCelsius: Double?
    ): ScanRecordEntity = coroutineScope {
        val bitmap = decodeBitmap(imageUri)
        val saveDeferred = async { saveBitmapToInternalStorage(bitmap) }
        val diagnosis = try {
            diagnosisService.analyzeLeaf(bitmap)
        } catch (t: Throwable) {
            runCatching {
                val path = saveDeferred.await()
                File(path).delete()
            }
            throw t
        }
        val persistedPath = saveDeferred.await()

        val record = ScanRecordEntity(
            userPhone = userPhone,
            imagePath = persistedPath,
            plantName = diagnosis.plantName,
            diseaseName = diagnosis.diseaseName,
            isHealthy = diagnosis.isHealthy,
            confidencePercent = diagnosis.confidencePercent,
            cause = diagnosis.cause,
            treatmentSteps = ScanRecordEntity.joinItems(diagnosis.treatmentSteps),
            recommendedProducts = ScanRecordEntity.joinItems(diagnosis.recommendedProducts),
            organicSolutions = ScanRecordEntity.joinItems(diagnosis.organicSolutions),
            preventionTips = ScanRecordEntity.joinItems(diagnosis.preventionTips),
            locationSummary = locationSummary,
            temperatureCelsius = temperatureCelsius
        )

        val insertedId = scanRecordDao.insertRecord(record)
        record.copy(id = insertedId)
    }

    suspend fun deleteRecord(record: ScanRecordEntity) {
        scanRecordDao.deleteRecordById(record.id)
        withContext(Dispatchers.IO) {
            runCatching {
                val file = File(record.imagePath)
                if (file.exists()) file.delete()
            }
        }
    }

    private suspend fun decodeBitmap(uri: Uri): Bitmap = withContext(Dispatchers.IO) {
        val maxDecodeDimension = 960
        val decoded = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val maxSide = maxOf(info.size.width, info.size.height)
                    if (maxSide > maxDecodeDimension) {
                        val scale = maxDecodeDimension.toFloat() / maxSide
                        decoder.setTargetSize(
                            (info.size.width * scale).toInt().coerceAtLeast(1),
                            (info.size.height * scale).toInt().coerceAtLeast(1)
                        )
                    }
                }
            }.getOrElse {
                decodeWithBitmapFactoryAndExif(uri, maxDecodeDimension)
            }
        } else {
            decodeWithBitmapFactoryAndExif(uri, maxDecodeDimension)
        }

        if (decoded.config == Bitmap.Config.ARGB_8888) {
            decoded
        } else {
            decoded.copy(Bitmap.Config.ARGB_8888, false)
        }
    }

    private fun decodeWithBitmapFactoryAndExif(uri: Uri, maxDecodeDimension: Int): Bitmap {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openUriStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOpts)
        }
        var inSample = 1
        val maxSide = maxOf(boundsOpts.outWidth, boundsOpts.outHeight)
        while (maxSide / inSample > maxDecodeDimension) {
            inSample *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = inSample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val raw = openUriStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
        } ?: throw IllegalStateException("Tidak dapat membuka file gambar yang dipilih.")

        val rotationDegrees = runCatching {
            openUriStream(uri)?.use { exifStream ->
                when (
                    ExifInterface(exifStream).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                ) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        }.getOrDefault(0f)

        if (rotationDegrees == 0f) return raw
        val matrix = Matrix().apply { postRotate(rotationDegrees) }
        return Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
    }

    private fun openUriStream(uri: Uri) =
        if (uri.scheme == "file" && uri.path != null) {
            File(uri.path!!).inputStream()
        } else {
            context.contentResolver.openInputStream(uri)
        }

    private suspend fun saveBitmapToInternalStorage(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "leaf_photos").apply { mkdirs() }
        val file = File(dir, "leaf_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 82, out)
        }
        file.absolutePath
    }
}
