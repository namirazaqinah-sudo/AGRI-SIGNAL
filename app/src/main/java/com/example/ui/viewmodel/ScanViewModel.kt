package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AgriDatabase
import com.example.data.local.ScanRecordEntity
import com.example.data.repository.LeafScanRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "ScanViewModel"

sealed interface ScanUiState {
    data object Idle : ScanUiState
    data class Analyzing(val current: Int = 1, val total: Int = 1) : ScanUiState
    data class Success(
        val record: ScanRecordEntity,
        val batchRecords: List<ScanRecordEntity> = listOf(record)
    ) : ScanUiState
    data class Error(val message: String) : ScanUiState
}

class ScanViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LeafScanRepository(
        context = application,
        scanRecordDao = AgriDatabase.getInstance(application).scanRecordDao()
    )

    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    private val _scanState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val scanState: StateFlow<ScanUiState> = _scanState.asStateFlow()

    private var analyzeJob: Job? = null

    fun setSelectedImage(uri: Uri) {
        analyzeJob?.cancel()
        _selectedImageUri.value = uri
        _scanState.value = ScanUiState.Idle
    }

    fun setSelectedImageAndAnalyze(
        uri: Uri,
        userPhone: String,
        locationSummary: String,
        temperatureCelsius: Double?
    ) {
        setSelectedImagesAndAnalyze(
            uris = listOf(uri),
            userPhone = userPhone,
            locationSummary = locationSummary,
            temperatureCelsius = temperatureCelsius
        )
    }

    fun setSelectedImagesAndAnalyze(
        uris: List<Uri>,
        userPhone: String,
        locationSummary: String,
        temperatureCelsius: Double?
    ) {
        if (uris.isEmpty()) return
        analyzeJob?.cancel()
        _selectedImageUri.value = uris.first()
        analyzeJob = viewModelScope.launch {
            val completed = mutableListOf<ScanRecordEntity>()
            var lastErrorMessage: String? = null

            uris.forEachIndexed { index, uri ->
                _selectedImageUri.value = uri
                _scanState.value = ScanUiState.Analyzing(current = index + 1, total = uris.size)
                try {
                    val savedRecord = repository.analyzeAndSaveLeaf(
                        userPhone = userPhone,
                        imageUri = uri,
                        locationSummary = locationSummary,
                        temperatureCelsius = temperatureCelsius
                    )
                    completed += savedRecord
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Leaf AI analysis failed for $uri", e)
                    lastErrorMessage = e.localizedMessage
                        ?: "Gagal menganalisis gambar daun. Silakan coba lagi."
                }
            }

            if (completed.isNotEmpty()) {
                _scanState.value = ScanUiState.Success(
                    record = completed.last(),
                    batchRecords = completed.toList()
                )
            } else {
                _scanState.value = ScanUiState.Error(
                    lastErrorMessage ?: "Gagal menganalisis gambar daun. Silakan coba lagi."
                )
            }
        }
    }

    fun analyzeSelectedLeaf(
        userPhone: String,
        locationSummary: String,
        temperatureCelsius: Double?
    ) {
        val uri = _selectedImageUri.value ?: return
        setSelectedImagesAndAnalyze(
            uris = listOf(uri),
            userPhone = userPhone,
            locationSummary = locationSummary,
            temperatureCelsius = temperatureCelsius
        )
    }

    fun selectBatchRecord(record: ScanRecordEntity) {
        val current = _scanState.value as? ScanUiState.Success ?: return
        _scanState.value = current.copy(record = record)
    }

    fun reset() {
        analyzeJob?.cancel()
        _selectedImageUri.value = null
        _scanState.value = ScanUiState.Idle
    }
}
