package com.santhomach.estateexpense.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.santhomach.estateexpense.data.model.PermanentWorker
import com.santhomach.estateexpense.data.model.WorkerPayment
import com.santhomach.estateexpense.ui.viewmodel.WorkerPaymentViewModel
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerPaymentScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: WorkerPaymentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val workers by viewModel.permanentWorkers.collectAsState()
    val payments by viewModel.allPayments.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedPaymentForEdit by remember { mutableStateOf<WorkerPayment?>(null) }
    var selectedPaymentForDelete by remember { mutableStateOf<WorkerPayment?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Worker Payments") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* All data is auto-saved */ }) {
                        Icon(Icons.Filled.Save, contentDescription = "Save")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Record Payment")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "Recent Payments",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            itemsIndexed(payments) { index, payment ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
                        initialOffsetY = { 50 },
                        animationSpec = tween(300, delayMillis = index * 50)
                    ),
                    exit = fadeOut(animationSpec = tween(200))
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = payment.workerName, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "₹${payment.amount}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = payment.paymentType, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    text = try {
                                        LocalDate.parse(payment.paymentDate).format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                                    } catch (e: Exception) {
                                        payment.paymentDate
                                    },
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            if (payment.notes.isNotEmpty()) {
                                Text(
                                    text = payment.notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Action buttons for edit/delete
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = {
                                    selectedPaymentForEdit = payment
                                    showEditDialog = true
                                }) {
                                    Text("Edit")
                                }
                                TextButton(
                                    onClick = {
                                        selectedPaymentForDelete = payment
                                        showDeleteDialog = true
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Delete")
                                }
                            }
                        }
                    }
                }
            }

            if (payments.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No payment records found")
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        RecordPaymentDialog(
            workers = workers,
            onDismiss = { showAddDialog = false },
            onConfirm = { workerId, amount, date, type, notes, existingId ->
                if (existingId == 0) {
                    viewModel.recordPayment(workerId, amount, date, type, notes)
                } else {
                    viewModel.updatePayment(existingId, workerId, amount, date, type, notes)
                }
                showAddDialog = false
            },
            onCheckExisting = { id, date -> viewModel.getExistingPayment(id, date) }
        )
    }

    selectedPaymentForEdit?.let { payment ->
        if (showEditDialog) {
            RecordPaymentDialog(
                workers = workers,
                onDismiss = {
                    showEditDialog = false
                    selectedPaymentForEdit = null
                },
                onConfirm = { workerId, amount, date, type, notes, _ ->
                    viewModel.updatePayment(payment.id, workerId, amount, date, type, notes)
                    showEditDialog = false
                    selectedPaymentForEdit = null
                },
                initialWorkerId = payment.workerId,
                initialAmount = payment.amount.toString(),
                initialDate = LocalDate.parse(payment.paymentDate),
                initialType = payment.paymentType,
                initialNotes = payment.notes,
                initialId = payment.id,
                onCheckExisting = { id, date -> viewModel.getExistingPayment(id, date) }
            )
        }
    }

    selectedPaymentForDelete?.let { payment ->
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Payment") },
                text = { Text("Are you sure you want to delete the payment record for ${payment.workerName} (${payment.amount}₹ on ${LocalDate.parse(payment.paymentDate).format(DateTimeFormatter.ofPattern("dd MMM yyyy"))})?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deletePayment(payment.id)
                            showDeleteDialog = false
                            selectedPaymentForDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    // Messages
    LaunchedEffect(uiState.successMessage, uiState.error) {
        if (uiState.successMessage != null || uiState.error != null) {
            // In a real app, use a Snackbar
            viewModel.clearMessages()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordPaymentDialog(
    workers: List<PermanentWorker>,
    onDismiss: () -> Unit,
    onConfirm: (Int, BigDecimal, LocalDate, String, String, Int) -> Unit, // Added ID for editing
    initialWorkerId: Int = 0,
    initialAmount: String = "",
    initialDate: LocalDate = LocalDate.now(),
    initialType: String = "MONTHLY",
    initialNotes: String = "",
    initialId: Int = 0,
    onCheckExisting: suspend (Int, LocalDate) -> WorkerPayment? = { _, _ -> null }
) {
    var paymentId by remember { mutableIntStateOf(initialId) }
    var selectedWorkerId by remember { mutableIntStateOf(initialWorkerId) }
    var amount by remember { mutableStateOf(initialAmount) }
    var date by remember { mutableStateOf(initialDate) }
    var paymentType by remember { mutableStateOf(initialType) }
    var notes by remember { mutableStateOf(initialNotes) }

    var showDatePicker by remember { mutableStateOf(false) }

    // Logic to check for existing payment when worker or date changes
    LaunchedEffect(selectedWorkerId, date) {
        if (selectedWorkerId > 0) {
            val existing = onCheckExisting(selectedWorkerId, date)
            if (existing != null && existing.id != paymentId) {
                // Load existing record for editing instead of creating new
                paymentId = existing.id
                amount = existing.amount.toString()
                paymentType = existing.paymentType
                notes = existing.notes
            } else if (existing == null && paymentId != 0 && initialId == 0) {
                // If it was auto-loaded but then changed to a non-existent combo, reset (only if we started fresh)
                paymentId = 0
                amount = ""
                notes = ""
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        date = java.time.Instant.ofEpochMilli(it)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (paymentId == 0) "Record Payment" else "Edit Payment") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Worker Selection
                var workerExpanded by remember { mutableStateOf(false) }
                val selectedWorker = workers.find { it.id == selectedWorkerId }

                ExposedDropdownMenuBox(
                    expanded = workerExpanded,
                    onExpandedChange = { workerExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedWorker?.name ?: "Select Worker",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Worker / Manager") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = workerExpanded) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = workerExpanded,
                        onDismissRequest = { workerExpanded = false }
                    ) {
                        workers.forEach { worker ->
                            DropdownMenuItem(
                                text = { Text("${worker.name} (${worker.role})") },
                                onClick = {
                                    selectedWorkerId = worker.id
                                    workerExpanded = false
                                }
                            )
                        }
                    }
                }

                // Date Selection
                OutlinedTextField(
                    value = date.format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Payment Date") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Select Date")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                // Type Selection
                var typeExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = paymentType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Payment Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        val paymentTypes = listOf("MONTHLY", "WEEKLY", "ADVANCE", "BONUS")
                        paymentTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    paymentType = type
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(selectedWorkerId, amount.toBigDecimalOrNull() ?: BigDecimal.ZERO, date, paymentType, notes, paymentId)
                },
                enabled = selectedWorkerId > 0 && amount.isNotEmpty()
            ) {
                Text(if (paymentId == 0) "Save" else "Update")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
