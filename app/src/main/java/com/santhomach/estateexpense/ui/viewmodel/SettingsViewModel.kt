package com.santhomach.estateexpense.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.santhomach.estateexpense.data.export.ExportManager
import com.santhomach.estateexpense.data.model.PermanentWorker
import com.santhomach.estateexpense.data.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val exportManager: ExportManager,
    private val repository: ExpenseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val permanentWorkers = repository.getAllActivePermanentWorkersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val workerTypes = repository.getAllActiveWorkerTypesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenseTypes = repository.getAllActiveExpenseTypesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val incomeTypes = repository.getAllActiveIncomeTypesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val workTasks = repository.getAllActiveWorkTasksFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addPermanentWorker(name: String, role: String, dailyWage: BigDecimal) {
        viewModelScope.launch {
            try {
                val worker = com.santhomach.estateexpense.data.model.PermanentWorker(
                    name = name,
                    role = role,
                    dailyBasicWage = dailyWage
                )
                repository.insertPermanentWorker(worker)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun addWorkerType(name: String, wage: BigDecimal) {
        viewModelScope.launch {
            try {
                repository.insertWorkerType(com.santhomach.estateexpense.data.model.WorkerType(workerTypeName = name, dailyBasicWage = wage))
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun addExpenseType(name: String) {
        viewModelScope.launch {
            try {
                repository.insertExpenseType(com.santhomach.estateexpense.data.model.ExpenseType(typeName = name))
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun addIncomeType(name: String) {
        viewModelScope.launch {
            try {
                repository.insertIncomeType(com.santhomach.estateexpense.data.model.IncomeType(typeName = name))
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun addWorkTask(name: String) {
        viewModelScope.launch {
            try {
                repository.insertWorkTask(com.santhomach.estateexpense.data.model.WorkTask(taskName = name))
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    suspend fun exportData(context: Context) {
        try {
            _uiState.update { it.copy(isExporting = true, error = null) }

            val exportedFile = exportManager.exportToFile()

            _uiState.update {
                it.copy(
                    isExporting = false,
                    exportMessage = "Data exported successfully to: ${exportedFile.absolutePath}"
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isExporting = false,
                    error = "Export failed: ${e.message}"
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(exportMessage = null, error = null) }
    }
}

data class SettingsUiState(
    val isExporting: Boolean = false,
    val exportMessage: String? = null,
    val error: String? = null
)
