package com.example.data.repository

import com.example.core.PhoneValidator
import com.example.data.local.FarmerAccountEntity
import com.example.data.local.FarmerDao
import com.example.data.local.SessionPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

sealed interface AuthResult {
    data class Success(val farmer: FarmerAccountEntity) : AuthResult
    data class ValidationError(val nameError: String? = null, val phoneError: String? = null) : AuthResult
    data class Failure(val message: String) : AuthResult
}

class AuthRepository(
    private val farmerDao: FarmerDao,
    private val sessionPreferences: SessionPreferences
) {
    val activeFarmerFlow: Flow<FarmerAccountEntity?> = sessionPreferences.activePhoneFlow.map { phone ->
        if (phone.isNullOrBlank()) null else farmerDao.getFarmerByPhone(phone)
    }

    suspend fun register(rawName: String, rawPhone: String): AuthResult {
        val nameError = PhoneValidator.validateName(rawName)
        val phoneError = PhoneValidator.validatePhone(rawPhone)
        if (nameError != null || phoneError != null) {
            return AuthResult.ValidationError(nameError = nameError, phoneError = phoneError)
        }

        val cleanName = rawName.trim()
        val normalizedPhone = PhoneValidator.normalizePhone(rawPhone)
        val existing = farmerDao.getFarmerByPhone(normalizedPhone)
        if (existing != null) {
            return AuthResult.Failure(
                "Nomor telepon sudah terdaftar atas nama ${existing.fullName}. Silakan masuk melalui tab Masuk."
            )
        }

        val entity = FarmerAccountEntity(
            phoneNumber = normalizedPhone,
            fullName = cleanName
        )
        farmerDao.insertFarmer(entity)
        sessionPreferences.saveLoggedInPhone(normalizedPhone)
        return AuthResult.Success(entity)
    }

    suspend fun login(rawName: String, rawPhone: String): AuthResult {
        val nameError = PhoneValidator.validateName(rawName)
        val phoneError = PhoneValidator.validatePhone(rawPhone)
        if (nameError != null || phoneError != null) {
            return AuthResult.ValidationError(nameError = nameError, phoneError = phoneError)
        }

        val cleanName = rawName.trim()
        val normalizedPhone = PhoneValidator.normalizePhone(rawPhone)
        val existing = farmerDao.getFarmerByPhone(normalizedPhone)
            ?: return AuthResult.Failure("Nomor telepon belum terdaftar. Silakan daftar terlebih dahulu.")

        if (!existing.fullName.equals(cleanName, ignoreCase = true)) {
            return AuthResult.Failure("Nama tidak cocok dengan nomor telepon yang terdaftar.")
        }

        sessionPreferences.saveLoggedInPhone(existing.phoneNumber)
        return AuthResult.Success(existing)
    }

    suspend fun getAllRegisteredFarmers(): List<FarmerAccountEntity> =
        farmerDao.getAllFarmers()

    suspend fun logout() {
        sessionPreferences.clearSession()
    }
}
