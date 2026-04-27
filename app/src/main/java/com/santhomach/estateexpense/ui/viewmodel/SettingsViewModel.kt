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

    fun updatePermanentWorker(worker: com.santhomach.estateexpense.data.model.PermanentWorker) {
        viewModelScope.launch {
            try {
                repository.updatePermanentWorker(worker)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deletePermanentWorker(worker: com.santhomach.estateexpense.data.model.PermanentWorker) {
        viewModelScope.launch {
            try {
                repository.deletePermanentWorker(worker)
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

    fun updateWorkerType(workerType: com.santhomach.estateexpense.data.model.WorkerType) {
        viewModelScope.launch {
            try {
                repository.updateWorkerType(workerType)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteWorkerType(workerType: com.santhomach.estateexpense.data.model.WorkerType) {
        viewModelScope.launch {
            try {
                repository.deleteWorkerType(workerType)
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

    fun updateExpenseType(expenseType: com.santhomach.estateexpense.data.model.ExpenseType) {
        viewModelScope.launch {
            try {
                repository.updateExpenseType(expenseType)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteExpenseType(expenseType: com.santhomach.estateexpense.data.model.ExpenseType) {
        viewModelScope.launch {
            try {
                repository.deleteExpenseType(expenseType)
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

    fun updateIncomeType(incomeType: com.santhomach.estateexpense.data.model.IncomeType) {
        viewModelScope.launch {
            try {
                repository.updateIncomeType(incomeType)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteIncomeType(incomeType: com.santhomach.estateexpense.data.model.IncomeType) {
        viewModelScope.launch {
            try {
                repository.deleteIncomeType(incomeType)
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

    fun updateWorkTask(task: com.santhomach.estateexpense.data.model.WorkTask) {
        viewModelScope.launch {
            try {
                repository.updateWorkTask(task)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteWorkTask(task: com.santhomach.estateexpense.data.model.WorkTask) {
        viewModelScope.launch {
            try {
                repository.deleteWorkTask(task)
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
