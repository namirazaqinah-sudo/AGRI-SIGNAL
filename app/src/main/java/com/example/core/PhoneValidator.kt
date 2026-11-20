package com.example.core

/**
 * Utility for validating and normalizing farmer names and Indonesian phone numbers.
 */
object PhoneValidator {

    fun validateName(rawName: String): String? {
        val trimmed = rawName.trim()
        return when {
            trimmed.isEmpty() -> "Nama lengkap tidak boleh kosong."
            trimmed.length < 2 -> "Nama minimal terdiri dari 2 karakter."
            else -> null
        }
    }

    /**
     * Normalizes spaces, dashes, and +62/62 prefixes into canonical "08..." format when applicable,
     * or returns digits only.
     */
    fun normalizePhone(rawPhone: String): String {
        val cleaned = rawPhone.trim().replace(Regex("[\\s\\-().]"), "")
        return when {
            cleaned.startsWith("+62") -> "0" + cleaned.removePrefix("+62")
            cleaned.startsWith("62") && cleaned.length >= 10 -> "0" + cleaned.removePrefix("62")
            else -> cleaned
        }
    }

    fun validatePhone(rawPhone: String): String? {
        val normalized = normalizePhone(rawPhone)
        return when {
            normalized.isEmpty() -> "Nomor telepon tidak boleh kosong."
            !normalized.all { it.isDigit() } -> "Nomor telepon hanya boleh berisi angka."
            normalized.length !in 10..15 -> "Nomor telepon harus terdiri dari 10 hingga 15 digit."
            !normalized.startsWith("0") -> "Nomor telepon harus diawali 08 atau +62."
            else -> null
        }
    }
}
