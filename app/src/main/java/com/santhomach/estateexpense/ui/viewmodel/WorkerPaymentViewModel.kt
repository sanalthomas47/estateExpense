package com.santhomach.estateexpense.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.santhomach.estateexpense.data.model.PermanentWorker
import com.santhomach.estateexpense.data.model.WorkerPayment
import com.santhomach.estateexpense.data.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class WorkerPaymentViewModel @Inject constructor(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkerPaymentUiState())
    val uiState: StateFlow<WorkerPaymentUiState> = _uiState.asStateFlow()

    val permanentWorkers = repository.getAllActivePermanentWorkersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPayments = repository.getAllPaymentsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun recordPayment(
        workerId: Int,
        amount: BigDecimal,
        date: LocalDate,
        paymentType: String,
        notes: String
    ) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSaving = true) }
                val worker = permanentWorkers.value.find { it.id == workerId }
                val payment = WorkerPayment(
                    workerId = workerId,
                    workerName = worker?.name ?: "Unknown",
                    amount = amount,
                    paymentDate = date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    paymentType = paymentType,
                    notes = notes
                )
                repository.insertWorkerPayment(payment)
                _uiState.update { it.copy(isSaving = false, successMessage = "Payment recorded successfully") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    fun updatePayment(
        id: Int,
        workerId: Int,
        amount: BigDecimal,
        date: LocalDate,
        paymentType: String,
        notes: String
    ) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSaving = true) }
                val existingPayment = allPayments.value.find { it.id == id }
                val worker = permanentWorkers.value.find { it.id == workerId }
                existingPayment?.let {
                    val updatedPayment = it.copy(
                        workerId = workerId,
                        workerName = worker?.name ?: "Unknown",
                        amount = amount,
                        paymentDate = date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                        paymentType = paymentType,
                        notes = notes
                    )
                    repository.updateWorkerPayment(updatedPayment)
                    _uiState.update { it.copy(isSaving = false, successMessage = "Payment updated successfully") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    fun deletePayment(id: Int) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isDeleting = true) }
                val existingPayment = allPayments.value.find { it.id == id }
                existingPayment?.let {
                    repository.deleteWorkerPayment(it)
                }
                _uiState.update { it.copy(isDeleting = false, successMessage = "Payment deleted successfully") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isDeleting = false, error = e.message) }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }
}

data class WorkerPaymentUiState(
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)
