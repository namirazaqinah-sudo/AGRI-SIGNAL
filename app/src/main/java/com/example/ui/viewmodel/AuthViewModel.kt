package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.FarmerExcelExporter
import com.example.core.RegistrationEmailDispatcher
import com.example.data.local.AgriDatabase
import com.example.data.local.FarmerAccountEntity
import com.example.data.local.SessionPreferences
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "AuthViewModel"

enum class AuthMode {
    REGISTER,
    LOGIN
}

sealed interface SessionState {
    data object Loading : SessionState
    data object Unauthenticated : SessionState
    data class Welcome(val farmer: FarmerAccountEntity) : SessionState
    data class Authenticated(val farmer: FarmerAccountEntity) : SessionState
}

data class AuthFormState(
    val mode: AuthMode = AuthMode.REGISTER,
    val fullName: String = "",
    val phoneNumber: String = "",
    val nameError: String? = null,
    val phoneError: String? = null,
    val formError: String? = null,
    val isSubmitting: Boolean = false
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository(
        farmerDao = AgriDatabase.getInstance(application).farmerDao(),
        sessionPreferences = SessionPreferences(application)
    )

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Loading)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private val _formState = MutableStateFlow(AuthFormState())
    val formState: StateFlow<AuthFormState> = _formState.asStateFlow()

    init {
        restoreSession()
    }

    private fun restoreSession() {
        viewModelScope.launch {
            val savedFarmer = authRepository.activeFarmerFlow.first()
            _sessionState.value = if (savedFarmer != null) {
                SessionState.Authenticated(savedFarmer)
            } else {
                SessionState.Unauthenticated
            }
        }
    }

    fun switchMode(mode: AuthMode) {
        _formState.update {
            it.copy(
                mode = mode,
                nameError = null,
                phoneError = null,
                formError = null
            )
        }
    }

    fun onNameChanged(value: String) {
        _formState.update {
            it.copy(fullName = value, nameError = null, formError = null)
        }
    }

    fun onPhoneChanged(value: String) {
        _formState.update {
            it.copy(phoneNumber = value, phoneError = null, formError = null)
        }
    }

    fun submit() {
        val current = _formState.value
        if (current.isSubmitting) return

        viewModelScope.launch {
            _formState.update { it.copy(isSubmitting = true, formError = null) }
            val isRegister = current.mode == AuthMode.REGISTER
            val result = if (isRegister) {
                authRepository.register(current.fullName, current.phoneNumber)
            } else {
                authRepository.login(current.fullName, current.phoneNumber)
            }

            when (result) {
                is AuthResult.Success -> {
                    if (isRegister) {
                        dispatchRegistrationEmailInBackground(result.farmer)
                    }
                    _formState.value = AuthFormState()
                    _sessionState.value = SessionState.Welcome(farmer = result.farmer)
                }

                is AuthResult.ValidationError -> {
                    _formState.update {
                        it.copy(
                            isSubmitting = false,
                            nameError = result.nameError,
                            phoneError = result.phoneError
                        )
                    }
                }

                is AuthResult.Failure -> {
                    _formState.update {
                        it.copy(
                            isSubmitting = false,
                            formError = result.message
                        )
                    }
                }
            }
        }
    }

    private fun dispatchRegistrationEmailInBackground(newFarmer: FarmerAccountEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val allFarmers = authRepository.getAllRegisteredFarmers()
                val file = FarmerExcelExporter.exportToFile(getApplication(), allFarmers)
                RegistrationEmailDispatcher.sendRegistrationExcel(
                    excelFile = file,
                    latestFarmer = newFarmer,
                    totalRegistered = allFarmers.size.coerceAtLeast(1)
                )
            }.onFailure { err ->
                Log.e(TAG, "Gagal memproses pengiriman otomatis data pendaftaran", err)
            }
        }
    }

    fun proceedFromWelcome() {
        val welcome = _sessionState.value as? SessionState.Welcome ?: return
        _sessionState.value = SessionState.Authenticated(welcome.farmer)
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _formState.value = AuthFormState(mode = AuthMode.LOGIN)
            _sessionState.value = SessionState.Unauthenticated
        }
    }
}
