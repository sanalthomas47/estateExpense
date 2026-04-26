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

import androidx.compose.ui.platform.LocalInspectionMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyExpenseScreen(
    date: LocalDate = LocalDate.now(),
    expenseId: Int? = null,
    onNavigateBack: () -> Unit = {},
    viewModelArg: DailyExpenseViewModel? = null
) {
    if (LocalInspectionMode.current && viewModelArg == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Daily Expense Screen Preview")
        }
        return
    }

    val viewModel: DailyExpenseViewModel = viewModelArg ?: hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val currentExpense by viewModel.currentExpense.collectAsState()
    val expenseTypes by viewModel.expenseTypes.collectAsState()
    val incomeTypes by viewModel.incomeTypes.collectAsState()
    val workerTypes by viewModel.workerTypes.collectAsState()
    val permanentWorkers by viewModel.permanentWorkers.collectAsState()
    val workTasks by viewModel.workTasks.collectAsState()

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

                    Spacer(modifier = Modifier.height(8.dp))

                    // Manager Selection
                    var managerExpanded by remember { mutableStateOf(false) }
                    val selectedManager = permanentWorkers.find { it.id == currentExpense?.managerId }

                    ExposedDropdownMenuBox(
                        expanded = managerExpanded,
                        onExpandedChange = { managerExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedManager?.name ?: "Select Manager (Optional)",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Estate Manager") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = managerExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = managerExpanded,
                            onDismissRequest = { managerExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("None") },
                                onClick = {
                                    viewModel.updateManager(null)
                                    managerExpanded = false
                                }
                            )
                            permanentWorkers.filter { it.role.contains("Manager", ignoreCase = true) }.forEach { manager ->
                                DropdownMenuItem(
                                    text = { Text(manager.name) },
                                    onClick = {
                                        viewModel.updateManager(manager.id)
                                        managerExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Worker Groups Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Worker Groups & Tasks",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val workerGroups = viewModel.getWorkerGroups()
                    workerGroups.forEachIndexed { index, group ->
                        WorkerGroupItemRow(
                            group = group,
                            onRemove = { viewModel.removeWorkerGroup(index) }
                        )
                        if (index < workerGroups.size - 1) {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }

                    if (workerGroups.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    AddWorkerGroupButton(
                        workerTypes = workerTypes,
                        workTasks = workTasks,
                        onAddGroup = { workerTypeId, count, wage, task, comments ->
                            viewModel.addWorkerGroup(workerTypeId, count, wage, task, comments)
                        },
                        onAddNewTask = { viewModel.addNewWorkTask(it) }
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
private fun WorkerGroupItemRow(
    group: com.santhomach.estateexpense.data.model.WorkerGroupEntry,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${group.workerTypeName} x ${group.count}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Task: ${group.taskPerformed}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            if (group.comments.isNotEmpty()) {
                Text(
                    text = group.comments,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "₹${group.wagePerDay * group.count.toBigDecimal()}",
                style = MaterialTheme.typography.bodyMedium
            )
            IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Filled.Save, // Should use a Delete icon, but Save is imported
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun AddWorkerGroupButton(
    workerTypes: List<com.santhomach.estateexpense.data.model.WorkerType>,
    workTasks: List<com.santhomach.estateexpense.data.model.WorkTask>,
    onAddGroup: (Int, Int, BigDecimal, String, String) -> Unit,
    onAddNewTask: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { showDialog = true },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Add Worker Group")
    }

    if (showDialog) {
        AddWorkerGroupDialog(
            workerTypes = workerTypes,
            workTasks = workTasks,
            onDismiss = { showDialog = false },
            onConfirm = { workerTypeId, count, wage, task, comments ->
                onAddGroup(workerTypeId, count, wage, task, comments)
                showDialog = false
            },
            onAddNewTask = onAddNewTask
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddWorkerGroupDialog(
    workerTypes: List<com.santhomach.estateexpense.data.model.WorkerType>,
    workTasks: List<com.santhomach.estateexpense.data.model.WorkTask>,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int, BigDecimal, String, String) -> Unit,
    onAddNewTask: (String) -> Unit
) {
    var selectedWorkerTypeId by remember { mutableStateOf(workerTypes.firstOrNull()?.id ?: 0) }
    var count by remember { mutableStateOf("1") }
    var wage by remember { 
        val defaultWage = workerTypes.firstOrNull()?.dailyBasicWage ?: BigDecimal.ZERO
        mutableStateOf(defaultWage.toString()) 
    }
    var selectedTask by remember { mutableStateOf(workTasks.firstOrNull()?.taskName ?: "") }
    var customTask by remember { mutableStateOf("") }
    var isCustomTask by remember { mutableStateOf(false) }
    var comments by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Worker Group") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Worker Type Dropdown
                var workerExpanded by remember { mutableStateOf(false) }
                val selectedWorkerType = workerTypes.find { it.id == selectedWorkerTypeId }

                ExposedDropdownMenuBox(
                    expanded = workerExpanded,
                    onExpandedChange = { workerExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedWorkerType?.workerTypeName ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Worker Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = workerExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = workerExpanded,
                        onDismissRequest = { workerExpanded = false }
                    ) {
                        workerTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.workerTypeName) },
                                onClick = {
                                    selectedWorkerTypeId = type.id
                                    wage = type.dailyBasicWage.toString()
                                    workerExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = count,
                        onValueChange = { if (it.all { char -> char.isDigit() }) count = it },
                        label = { Text("Count") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = wage,
                        onValueChange = { wage = it },
                        label = { Text("Daily Wage") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1.5f)
                    )
                }

                // Task Selection
                var taskExpanded by remember { mutableStateOf(false) }
                val taskOptions = workTasks.map { it.taskName } + "Other (Enter New)"

                ExposedDropdownMenuBox(
                    expanded = taskExpanded,
                    onExpandedChange = { taskExpanded = it }
                ) {
                    OutlinedTextField(
                        value = if (isCustomTask) "Other (Enter New)" else selectedTask,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Work Performed") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = taskExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = taskExpanded,
                        onDismissRequest = { taskExpanded = false }
                    ) {
                        taskOptions.forEach { task ->
                            DropdownMenuItem(
                                text = { Text(task) },
                                onClick = {
                                    if (task == "Other (Enter New)") {
                                        isCustomTask = true
                                    } else {
                                        isCustomTask = false
                                        selectedTask = task
                                    }
                                    taskExpanded = false
                                }
                            )
                        }
                    }
                }

                if (isCustomTask) {
                    OutlinedTextField(
                        value = customTask,
                        onValueChange = { customTask = it },
                        label = { Text("Enter New Task Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = comments,
                    onValueChange = { comments = it },
                    label = { Text("Comments") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalTask = if (isCustomTask) customTask else selectedTask
                    if (isCustomTask && customTask.isNotEmpty()) {
                        onAddNewTask(customTask)
                    }
                    onConfirm(
                        selectedWorkerTypeId,
                        count.toIntOrNull() ?: 0,
                        wage.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                        finalTask,
                        comments
                    )
                },
                enabled = (isCustomTask && customTask.isNotEmpty()) || (!isCustomTask && selectedTask.isNotEmpty())
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
