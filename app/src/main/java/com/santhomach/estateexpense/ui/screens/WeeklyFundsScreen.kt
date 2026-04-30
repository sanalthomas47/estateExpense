package com.santhomach.estateexpense.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.santhomach.estateexpense.data.model.WeeklyFunds
import com.santhomach.estateexpense.ui.viewmodel.WeeklyFundsViewModel
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyFundsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: WeeklyFundsViewModel = hiltViewModel()
) {
    val weeklyFunds by viewModel.weeklyFunds.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedFundsForEdit by remember { mutableStateOf<WeeklyFunds?>(null) }
    var selectedFundsForDelete by remember { mutableStateOf<WeeklyFunds?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weekly Funds Tracking") },
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
                Icon(Icons.Filled.Add, contentDescription = "Add Funds")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Weekly Funds vs Actual Expenses",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            items(weeklyFunds) { funds ->
                val startDate = LocalDate.parse(funds.weekStartDate)
                val comparison by viewModel.getComparisonFlow(startDate).collectAsState(initial = null)

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Week of ${startDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))}",
                            style = MaterialTheme.typography.labelLarge
                        )
                        
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        SummaryRow("Funds Received", "₹${funds.amountReceived}")
                        
                        if (funds.paymentMade > BigDecimal.ZERO) {
                            SummaryRow("Payment Made", "₹${funds.paymentMade}")
                        }

                        comparison?.let { comp ->
                            SummaryRow("Total Expenses", "₹${comp.totalExpenses}")
                            
                            val netBalance = funds.paymentMade - comp.totalExpenses
                            SummaryRow(
                                label = if (netBalance >= BigDecimal.ZERO) "Excess Balance" else "Shortfall",
                                value = "₹${netBalance.abs()}",
                                isPositive = netBalance >= BigDecimal.ZERO
                            )

                            if (netBalance > BigDecimal.ZERO) {
                                Text(
                                    text = "This excess will carry over to next week",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        } ?: CircularProgressIndicator(modifier = Modifier.size(16.dp))

                        if (funds.notes.isNotEmpty()) {
                            Text(
                                text = "Notes: ${funds.notes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        // Action buttons for edit/delete
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { 
                                selectedFundsForEdit = funds
                                showEditDialog = true 
                            }) {
                                Text("Edit")
                            }
                            TextButton(
                                onClick = { 
                                    selectedFundsForDelete = funds
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
    }

    if (showAddDialog) {
        AddFundsDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { amount, paymentMade, date, notes ->
                viewModel.addFunds(amount, paymentMade, date, notes)
                showAddDialog = false
            }
        )
    }

    selectedFundsForEdit?.let { funds ->
        if (showEditDialog) {
            AddFundsDialog(
                onDismiss = { 
                    showEditDialog = false
                    selectedFundsForEdit = null
                },
                onConfirm = { amount, paymentMade, date, notes ->
                    viewModel.updateFunds(funds.id, amount, paymentMade, notes)
                    showEditDialog = false
                    selectedFundsForEdit = null
                },
                initialAmount = funds.amountReceived.toString(),
                initialPaymentMade = funds.paymentMade.toString(),
                initialDate = LocalDate.parse(funds.weekStartDate),
                initialNotes = funds.notes
            )
        }
    }

    selectedFundsForDelete?.let { funds ->
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Funds Record") },
                text = { Text("Are you sure you want to delete the funds record for the week of ${LocalDate.parse(funds.weekStartDate).format(DateTimeFormatter.ofPattern("dd MMM yyyy"))}?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteFunds(funds)
                            showDeleteDialog = false
                            selectedFundsForDelete = null
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
}

@Composable
private fun SummaryRow(label: String, value: String, isPositive: Boolean? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = when (isPositive) {
                true -> MaterialTheme.colorScheme.primary
                false -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFundsDialog(
    onDismiss: () -> Unit,
    onConfirm: (BigDecimal, BigDecimal, LocalDate, String) -> Unit,
    initialAmount: String = "",
    initialPaymentMade: String = "",
    initialDate: LocalDate = LocalDate.now(),
    initialNotes: String = ""
) {
    var amount by remember { mutableStateOf(initialAmount) }
    var paymentMade by remember { mutableStateOf(initialPaymentMade) }
    var notes by remember { mutableStateOf(initialNotes) }
    var date by remember { mutableStateOf(initialDate) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Funds Received") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount Received (₹)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = paymentMade,
                    onValueChange = { paymentMade = it },
                    label = { Text("Payment Made (₹) - Thursday") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text(
                    text = "Select any day in the week. App will automatically map it to the start of that week (Monday).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )

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
                    onConfirm(
                        amount.toBigDecimalOrNull() ?: BigDecimal.ZERO, 
                        paymentMade.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                        date, 
                        notes
                    ) 
                },
                enabled = amount.isNotEmpty()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
