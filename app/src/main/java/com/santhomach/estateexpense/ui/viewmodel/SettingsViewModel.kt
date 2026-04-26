package com.santhomach.estateexpense.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.santhomach.estateexpense.data.export.ExportManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val exportManager: ExportManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

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
