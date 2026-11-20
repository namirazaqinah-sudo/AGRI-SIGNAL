package com.example.ui.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.EnvironmentRepository
import com.example.data.repository.EnvironmentSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "EnvironmentViewModel"

sealed interface EnvironmentUiState {
    data object PermissionRequired : EnvironmentUiState
    data object Loading : EnvironmentUiState
    data class Success(val snapshot: EnvironmentSnapshot) : EnvironmentUiState
    data class Error(val message: String) : EnvironmentUiState
}

class EnvironmentViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = EnvironmentRepository(application)

    private val _uiState = MutableStateFlow<EnvironmentUiState>(EnvironmentUiState.Loading)
    val uiState: StateFlow<EnvironmentUiState> = _uiState.asStateFlow()

    init {
        checkPermissionAndLoad()
    }

    fun hasLocationPermission(): Boolean {
        val ctx = getApplication<Application>()
        val fine = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun checkPermissionAndLoad() {
        if (hasLocationPermission()) {
            refreshEnvironment()
        } else {
            _uiState.value = EnvironmentUiState.PermissionRequired
        }
    }

    fun onPermissionResult(granted: Boolean) {
        if (granted || hasLocationPermission()) {
            refreshEnvironment()
        } else {
            _uiState.value = EnvironmentUiState.PermissionRequired
        }
    }

    fun refreshEnvironment() {
        if (!hasLocationPermission()) {
            _uiState.value = EnvironmentUiState.PermissionRequired
            return
        }
        viewModelScope.launch {
            _uiState.value = EnvironmentUiState.Loading
            try {
                val snapshot = repository.fetchCurrentEnvironment()
                _uiState.value = EnvironmentUiState.Success(snapshot)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch environment data", e)
                _uiState.value = EnvironmentUiState.Error(
                    e.localizedMessage ?: "Gagal memuat data cuaca dan lokasi. Periksa koneksi internet dan GPS Anda."
                )
            }
        }
    }
}
