package com.santhomach.estateexpense.ui.viewmodel

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
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
    private val repository: ExpenseRepository,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val themeMode: StateFlow<ThemeMode> = dataStore.data
        .map { prefs ->
            when (prefs[THEME_KEY]) {
                "LIGHT" -> ThemeMode.LIGHT
                "DARK" -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            dataStore.edit { it[THEME_KEY] = mode.name }
        }
    }

    companion object {
        private val THEME_KEY = stringPreferencesKey("theme_mode")
    }

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

    val expenseSubtypes = repository.getAllActiveExpenseSubtypesFlow()
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

    fun deleteExpenseSubtype(expenseSubtype: com.santhomach.estateexpense.data.model.ExpenseSubtype) {
        viewModelScope.launch {
            try {
                repository.deleteExpenseSubtype(expenseSubtype)
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

            val exportPath = exportManager.exportToFile()

            _uiState.update {
                it.copy(
                    isExporting = false,
                    exportMessage = "Data exported successfully to: $exportPath"
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

    suspend fun importData(uri: android.net.Uri, context: Context) {
        try {
            _uiState.update { it.copy(isExporting = true, error = null) }

            val jsonString = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: throw Exception("Could not read file")

            exportManager.importFromJson(jsonString)

            _uiState.update {
                it.copy(
                    isExporting = false,
                    exportMessage = "Data imported successfully. Please restart the app if changes don't appear immediately."
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isExporting = false,
                    error = "Import failed: ${e.message}"
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

enum class ThemeMode(val label: String) {
    SYSTEM("System"), LIGHT("Light"), DARK("Dark")
}
