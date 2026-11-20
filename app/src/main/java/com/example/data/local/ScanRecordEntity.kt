package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_records")
data class ScanRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val userPhone: String,
    val imagePath: String,
    val plantName: String,
    val diseaseName: String,
    val isHealthy: Boolean,
    val confidencePercent: Int,
    val cause: String,
    val treatmentSteps: String,
    val recommendedProducts: String,
    val organicSolutions: String,
    val preventionTips: String,
    val locationSummary: String,
    val temperatureCelsius: Double?,
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    fun treatmentList(): List<String> = splitItems(treatmentSteps)
    fun productList(): List<String> = splitItems(recommendedProducts)
    fun organicList(): List<String> = splitItems(organicSolutions)
    fun preventionList(): List<String> = splitItems(preventionTips)

    companion object {
        private const val DELIMITER = "\n•|"

        fun joinItems(items: List<String>): String =
            items.map { it.trim() }.filter { it.isNotEmpty() }.joinToString(DELIMITER)

        fun splitItems(encoded: String): List<String> =
            if (encoded.isBlank()) emptyList() else encoded.split(DELIMITER).filter { it.isNotBlank() }
    }
}
