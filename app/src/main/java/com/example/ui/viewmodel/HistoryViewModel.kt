package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AgriDatabase
import com.example.data.local.ScanRecordEntity
import com.example.data.repository.LeafScanRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class HistoryFilter(val label: String) {
    ALL("Semua"),
    DISEASE("Penyakit"),
    HEALTHY("Daun Sehat")
}

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LeafScanRepository(
        context = application,
        scanRecordDao = AgriDatabase.getInstance(application).scanRecordDao()
    )

    private val activePhone = MutableStateFlow<String?>(null)

    private val _filter = MutableStateFlow(HistoryFilter.ALL)
    val filter: StateFlow<HistoryFilter> = _filter.asStateFlow()

    private val _selectedRecord = MutableStateFlow<ScanRecordEntity?>(null)
    val selectedRecord: StateFlow<ScanRecordEntity?> = _selectedRecord.asStateFlow()

    val allRecords: StateFlow<List<ScanRecordEntity>> = activePhone
        .flatMapLatest { phone ->
            if (phone.isNullOrBlank()) flowOf(emptyList())
            else repository.observeHistoryForUser(phone)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val filteredRecords: StateFlow<List<ScanRecordEntity>> = combine(
        allRecords,
        _filter
    ) { list, filter ->
        when (filter) {
            HistoryFilter.ALL -> list
            HistoryFilter.DISEASE -> list.filter { !it.isHealthy }
            HistoryFilter.HEALTHY -> list.filter { it.isHealthy }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    fun setActiveUserPhone(phone: String) {
        if (activePhone.value != phone) {
            activePhone.value = phone
        }
    }

    fun setFilter(filter: HistoryFilter) {
        _filter.value = filter
    }

    fun openDetail(record: ScanRecordEntity) {
        _selectedRecord.value = record
    }

    fun closeDetail() {
        _selectedRecord.value = null
    }

    fun deleteRecord(record: ScanRecordEntity) {
        viewModelScope.launch {
            if (_selectedRecord.value?.id == record.id) {
                _selectedRecord.value = null
            }
            repository.deleteRecord(record)
        }
    }
}
