package com.santhomach.estateexpense.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.santhomach.estateexpense.data.model.OtherExpenseEntry
import com.santhomach.estateexpense.data.model.IncomeEntry
import com.santhomach.estateexpense.ui.viewmodel.DailyExpenseViewModel
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyExpenseScreen(
    date: LocalDate = LocalDate.now(),
    expenseId: Int? = null,
    onNavigateBack: () -> Unit = {},
    viewModel: DailyExpenseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentExpense by viewModel.currentExpense.collectAsState()
    val expenseTypes by viewModel.expenseTypes.collectAsState()
    val incomeTypes by viewModel.incomeTypes.collectAsState()
    val workerTypes by viewModel.workerTypes.collectAsState()
    val permanentWorkers by viewModel.permanentWorkers.collectAsState()

    val scrollState = rememberScrollState()

    // Load expense if editing
    LaunchedEffect(expenseId) {
        if (expenseId != null) {
            viewModel.loadExpense(expenseId)
        } else {
            viewModel.createNewExpense(date)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (expenseId != null) "Edit Expense" else "Add Daily Expense") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.isEditing) {
                        IconButton(onClick = { viewModel.saveExpense() }) {
                            Icon(Icons.Filled.Save, contentDescription = "Save")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Date Display
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Date: ${date.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))}",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            // Worker Counts Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Worker Counts",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    WorkerCountInput(
                        label = "Malayalam Male",
                        count = currentExpense?.malayaliMaleCount ?: 0,
                        wage = currentExpense?.malayaliMaleWagePerDay ?: BigDecimal.ZERO,
                        onCountChange = { viewModel.updateWorkerCount("Malayalam Male", it) },
                        onWageChange = { viewModel.updateWage("Malayalam Male", it) }
                    )

                    WorkerCountInput(
                        label = "Bengali Male",
                        count = currentExpense?.bengaliMaleCount ?: 0,
                        wage = currentExpense?.bengaliMaleWagePerDay ?: BigDecimal.ZERO,
                        onCountChange = { viewModel.updateWorkerCount("Bengali Male", it) },
                        onWageChange = { viewModel.updateWage("Bengali Male", it) }
                    )

                    WorkerCountInput(
                        label = "Malayalam Female",
                        count = currentExpense?.malayaliFemaleCount ?: 0,
                        wage = currentExpense?.malayaliFemaleWagePerDay ?: BigDecimal.ZERO,
                        onCountChange = { viewModel.updateWorkerCount("Malayalam Female", it) },
                        onWageChange = { viewModel.updateWage("Malayalam Female", it) }
                    )

                    WorkerCountInput(
                        label = "Bengali Female",
                        count = currentExpense?.bengaliFemaleCount ?: 0,
                        wage = currentExpense?.bengaliFemaleWagePerDay ?: BigDecimal.ZERO,
                        onCountChange = { viewModel.updateWorkerCount("Bengali Female", it) },
                        onWageChange = { viewModel.updateWage("Bengali Female", it) }
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        text = "Total Labor Cost: ₹${viewModel.getTotalCost()}",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }

            // Overtime Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Overtime",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = (currentExpense?.overtimeHours ?: 0).toString(),
                        onValueChange = { viewModel.updateOvertime(it.toIntOrNull() ?: 0) },
                        label = { Text("Overtime Hours") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Overtime Cost: ₹${currentExpense?.totalOvertimeCost ?: BigDecimal.ZERO}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Other Expenses Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Other Expenses",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val otherExpenses = viewModel.getOtherExpenses()
                    otherExpenses.forEachIndexed { index, expense ->
                        ExpenseItemRow(
                            expense = expense,
                            onRemove = { viewModel.removeOtherExpense(index) }
                        )
                    }

                    AddExpenseButton(
                        expenseTypes = expenseTypes,
                        onAddExpense = { typeId, amount, notes ->
                            viewModel.addOtherExpense(typeId, amount, notes)
                        }
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        text = "Total Other Expenses: ₹${currentExpense?.totalOtherExpensesCost ?: BigDecimal.ZERO}",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }

            // Income Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Income",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val incomeEntries = viewModel.getIncomeEntries()
                    incomeEntries.forEachIndexed { index, income ->
                        IncomeItemRow(
                            income = income,
                            onRemove = { viewModel.removeIncome(index) }
                        )
                    }

                    AddIncomeButton(
                        incomeTypes = incomeTypes,
                        onAddIncome = { typeId, amount, notes ->
                            viewModel.addIncome(typeId, amount, notes)
                        }
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        text = "Total Income: ₹${currentExpense?.totalIncome ?: BigDecimal.ZERO}",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }

            // Excess Balance Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Excess Balance",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = (currentExpense?.excessBalance ?: BigDecimal.ZERO).toString(),
                        onValueChange = { viewModel.updateExcessBalance(it.toBigDecimalOrNull() ?: BigDecimal.ZERO) },
                        label = { Text("Excess Balance (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Comments Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Comments",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = currentExpense?.comments ?: "",
                        onValueChange = { viewModel.updateComments(it) },
                        label = { Text("Comments") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            }

            // Summary Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Summary",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    SummaryRow("Total Expenses", "₹${viewModel.getTotalCost()}")
                    SummaryRow("Total Income", "₹${currentExpense?.totalIncome ?: BigDecimal.ZERO}")
                    SummaryRow("Net Amount", "₹${viewModel.getNetAmount()}")
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.cancelEditing(); onNavigateBack() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = { viewModel.saveExpense(); onNavigateBack() },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isSaving
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    } else {
                        Text("Save")
                    }
                }
            }

            // Error Display
            uiState.error?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkerCountInput(
    label: String,
    count: Int,
    wage: BigDecimal,
    onCountChange: (Int) -> Unit,
    onWageChange: (BigDecimal) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium
        )

        OutlinedTextField(
            value = count.toString(),
            onValueChange = { onCountChange(it.toIntOrNull() ?: 0) },
            label = { Text("Count") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(80.dp)
        )

        OutlinedTextField(
            value = wage.toString(),
            onValueChange = { onWageChange(it.toBigDecimalOrNull() ?: BigDecimal.ZERO) },
            label = { Text("Rate") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.width(100.dp)
        )

        Text(
            text = "₹${wage * count.toBigDecimal()}",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(80.dp)
        )
    }
}

@Composable
private fun ExpenseItemRow(
    expense: OtherExpenseEntry,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = expense.typeName, style = MaterialTheme.typography.bodyMedium)
            if (expense.notes.isNotEmpty()) {
                Text(text = expense.notes, style = MaterialTheme.typography.bodySmall)
            }
        }
        Text(text = "₹${expense.amount}", style = MaterialTheme.typography.bodyMedium)
        IconButton(onClick = onRemove) {
            // Remove icon
        }
    }
}

@Composable
private fun IncomeItemRow(
    income: IncomeEntry,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = income.typeName, style = MaterialTheme.typography.bodyMedium)
            if (income.notes.isNotEmpty()) {
                Text(text = income.notes, style = MaterialTheme.typography.bodySmall)
            }
        }
        Text(text = "₹${income.amount}", style = MaterialTheme.typography.bodyMedium)
        IconButton(onClick = onRemove) {
            // Remove icon
        }
    }
}

@Composable
private fun AddExpenseButton(
    expenseTypes: List<com.santhomach.estateexpense.data.model.ExpenseType>,
    onAddExpense: (Int, BigDecimal, String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { showDialog = true },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Add Expense")
    }

    if (showDialog) {
        AddExpenseDialog(
            expenseTypes = expenseTypes,
            onDismiss = { showDialog = false },
            onConfirm = { typeId, amount, notes ->
                onAddExpense(typeId, amount, notes)
                showDialog = false
            }
        )
    }
}

@Composable
private fun AddIncomeButton(
    incomeTypes: List<com.santhomach.estateexpense.data.model.IncomeType>,
    onAddIncome: (Int, BigDecimal, String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { showDialog = true },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Add Income")
    }

    if (showDialog) {
        AddIncomeDialog(
            incomeTypes = incomeTypes,
            onDismiss = { showDialog = false },
            onConfirm = { typeId, amount, notes ->
                onAddIncome(typeId, amount, notes)
                showDialog = false
            }
        )
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExpenseDialog(
    expenseTypes: List<com.santhomach.estateexpense.data.model.ExpenseType>,
    onDismiss: () -> Unit,
    onConfirm: (Int, BigDecimal, String) -> Unit
) {
    var selectedTypeId by remember { mutableStateOf(expenseTypes.firstOrNull()?.id ?: 0) }
    var amount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Expense") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Expense Type Dropdown
                var expanded by remember { mutableStateOf(false) }
                val selectedType = expenseTypes.find { it.id == selectedTypeId }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedType?.typeName ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Expense Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        expenseTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.typeName) },
                                onClick = {
                                    selectedTypeId = type.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountValue = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    onConfirm(selectedTypeId, amountValue, notes)
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddIncomeDialog(
    incomeTypes: List<com.santhomach.estateexpense.data.model.IncomeType>,
    onDismiss: () -> Unit,
    onConfirm: (Int, BigDecimal, String) -> Unit
) {
    var selectedTypeId by remember { mutableStateOf(incomeTypes.firstOrNull()?.id ?: 0) }
    var amount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Income") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Income Type Dropdown
                var expanded by remember { mutableStateOf(false) }
                val selectedType = incomeTypes.find { it.id == selectedTypeId }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedType?.typeName ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Income Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        incomeTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.typeName) },
                                onClick = {
                                    selectedTypeId = type.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountValue = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    onConfirm(selectedTypeId, amountValue, notes)
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
