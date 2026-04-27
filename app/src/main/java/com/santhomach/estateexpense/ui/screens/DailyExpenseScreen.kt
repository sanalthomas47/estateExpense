package com.santhomach.estateexpense.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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

    var shouldNavigateBack by remember { mutableStateOf(false) }

    // State for editing items
    var editingWorkerGroupIndex by remember { mutableStateOf<Int?>(null) }
    var showAddWorkerGroupDialog by remember { mutableStateOf(false) }

    var editingOtherExpenseIndex by remember { mutableStateOf<Int?>(null) }
    var showAddOtherExpenseDialog by remember { mutableStateOf(false) }

    var editingIncomeIndex by remember { mutableStateOf<Int?>(null) }
    var showAddIncomeDialog by remember { mutableStateOf(false) }

    val workerGroups = remember(currentExpense) {
        viewModel.getWorkerGroups()
    }

    val otherExpenses = remember(currentExpense) {
        viewModel.getOtherExpenses()
    }

    val incomeEntries = remember(currentExpense) {
        viewModel.getIncomeEntries()
    }

    // Load expense if editing
    LaunchedEffect(expenseId) {
        if (expenseId != null) {
            viewModel.loadExpense(expenseId)
        } else {
            viewModel.createNewExpense(date)
        }
    }

    // Navigate back after successful save
    LaunchedEffect(shouldNavigateBack, uiState.isSaving) {
        if (shouldNavigateBack && !uiState.isSaving) {
            shouldNavigateBack = false
            onNavigateBack()
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
                    if (expenseId != null) {
                        var showDeleteDialog by remember { mutableStateOf(false) }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        if (showDeleteDialog) {
                            AlertDialog(
                                onDismissRequest = { showDeleteDialog = false },
                                title = { Text("Delete Expense") },
                                text = { Text("Are you sure you want to delete this expense record?") },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            viewModel.deleteExpense()
                                            onNavigateBack()
                                        },
                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
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
                    if (uiState.isEditing) {
                        IconButton(onClick = { shouldNavigateBack = true; viewModel.saveExpense() }) {
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
            // Manager Selection
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "General Details",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Date: ${date.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = (currentExpense?.excessBalance ?: BigDecimal.ZERO).toString(),
                        onValueChange = { viewModel.updateExcessBalance(it.toBigDecimalOrNull() ?: BigDecimal.ZERO) },
                        label = { Text("Previous Excess Balance (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

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

                    workerGroups.forEachIndexed { index, group ->
                        WorkerGroupItemRow(
                            group = group,
                            onRemove = { viewModel.removeWorkerGroup(index) },
                            onEdit = { editingWorkerGroupIndex = index }
                        )
                        if (index < workerGroups.size - 1) {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }

                    if (workerGroups.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    OutlinedButton(
                        onClick = { showAddWorkerGroupDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Worker Group")
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        text = "Total Labor Cost: ₹${currentExpense?.totalLaborCost ?: BigDecimal.ZERO}",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }

            // Overtime Section (Legacy - but keeping for compatibility)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Extra Overtime",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = (currentExpense?.overtimeHours ?: 0).toString(),
                        onValueChange = { viewModel.updateOvertime(it.toIntOrNull() ?: 0) },
                        label = { Text("Total Overtime Hours") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Calculated Cost: ₹${currentExpense?.totalOvertimeCost ?: BigDecimal.ZERO}",
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

                    otherExpenses.forEachIndexed { index, expense ->
                        ExpenseItemRow(
                            expense = expense,
                            onRemove = { viewModel.removeOtherExpense(index) },
                            onEdit = { editingOtherExpenseIndex = index }
                        )
                    }

                    OutlinedButton(
                        onClick = { showAddOtherExpenseDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Expense")
                    }

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

                    incomeEntries.forEachIndexed { index, income ->
                        IncomeItemRow(
                            income = income,
                            onRemove = { viewModel.removeIncome(index) },
                            onEdit = { editingIncomeIndex = index }
                        )
                    }

                    OutlinedButton(
                        onClick = { showAddIncomeDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Income")
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        text = "Total Income: ₹${currentExpense?.totalIncome ?: BigDecimal.ZERO}",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }

            // Advance Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Adjustments",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = (currentExpense?.advanceAmount ?: BigDecimal.ZERO).toString(),
                        onValueChange = { viewModel.updateAdvanceAmount(it.toBigDecimalOrNull() ?: BigDecimal.ZERO) },
                        label = { Text("Advance Paid (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = currentExpense?.advanceReason ?: "",
                        onValueChange = { viewModel.updateAdvanceReason(it) },
                        label = { Text("Reason for Advance (Comment)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            }

            // Comments Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Daily Comments",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = currentExpense?.comments ?: "",
                        onValueChange = { viewModel.updateComments(it) },
                        label = { Text("General comments for the day") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            }

            // Summary Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Daily Summary",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (workerGroups.isNotEmpty()) {
                        Text(
                            text = "Labor Summary:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        workerGroups.forEach { group ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${group.workerTypeName} (${group.count}) - ${group.taskPerformed}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "₹${group.calculateTotalGroupCost()}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                    }

                    SummaryRow("Total Labor", "₹${currentExpense?.totalLaborCost ?: BigDecimal.ZERO}")
                    SummaryRow("Total Overtime", "₹${currentExpense?.totalOvertimeCost ?: BigDecimal.ZERO}")
                    SummaryRow("Advance Amount", "₹${currentExpense?.advanceAmount ?: BigDecimal.ZERO}")
                    SummaryRow("Other Expenses", "₹${currentExpense?.totalOtherExpensesCost ?: BigDecimal.ZERO}")
                    SummaryRow("Previous Excess Balance", "₹${currentExpense?.excessBalance ?: BigDecimal.ZERO}")
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    SummaryRow("TOTAL EXPENSES", "₹${viewModel.getTotalCost()}", isTotal = true)
                    SummaryRow("TOTAL INCOME", "₹${currentExpense?.totalIncome ?: BigDecimal.ZERO}", isTotal = true)
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
                    onClick = { shouldNavigateBack = true; viewModel.saveExpense() },
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

    // Dialogs
    if (showAddWorkerGroupDialog || editingWorkerGroupIndex != null) {
        val initialGroup = editingWorkerGroupIndex?.let { workerGroups.getOrNull(it) }
        AddWorkerGroupDialog(
            workerTypes = workerTypes,
            workTasks = workTasks,
            onDismiss = { 
                showAddWorkerGroupDialog = false
                editingWorkerGroupIndex = null
            },
            onConfirm = { workerTypeId, count, wage, otHours, otWage, task, comments ->
                if (editingWorkerGroupIndex != null) {
                    viewModel.updateWorkerGroup(editingWorkerGroupIndex!!, workerTypeId, count, wage, otHours, otWage, task, comments)
                } else {
                    viewModel.addWorkerGroup(workerTypeId, count, wage, otHours, otWage, task, comments)
                }
                showAddWorkerGroupDialog = false
                editingWorkerGroupIndex = null
            },
            onAddNewTask = { viewModel.addNewWorkTask(it) },
            initialGroup = initialGroup
        )
    }

    if (showAddOtherExpenseDialog || editingOtherExpenseIndex != null) {
        val initialExpense = editingOtherExpenseIndex?.let { otherExpenses.getOrNull(it) }
        AddExpenseDialog(
            expenseTypes = expenseTypes,
            onDismiss = { 
                showAddOtherExpenseDialog = false
                editingOtherExpenseIndex = null
            },
            onConfirm = { typeId, amount, quantity, notes ->
                if (editingOtherExpenseIndex != null) {
                    viewModel.updateOtherExpense(editingOtherExpenseIndex!!, typeId, amount, quantity, notes)
                } else {
                    viewModel.addOtherExpense(typeId, amount, quantity, notes)
                }
                showAddOtherExpenseDialog = false
                editingOtherExpenseIndex = null
            },
            onAddNewType = { viewModel.addNewExpenseType(it) },
            initialExpense = initialExpense
        )
    }

    if (showAddIncomeDialog || editingIncomeIndex != null) {
        val initialIncome = editingIncomeIndex?.let { incomeEntries.getOrNull(it) }
        AddIncomeDialog(
            incomeTypes = incomeTypes,
            onDismiss = { 
                showAddIncomeDialog = false
                editingIncomeIndex = null
            },
            onConfirm = { typeId, amount, weight, price, transport, notes ->
                if (editingIncomeIndex != null) {
                    viewModel.updateIncome(editingIncomeIndex!!, typeId, amount, weight, price, transport, notes)
                } else {
                    viewModel.addIncome(typeId, amount, weight, price, transport, notes)
                }
                showAddIncomeDialog = false
                editingIncomeIndex = null
            },
            onAddNewType = { viewModel.addNewIncomeType(it) },
            initialIncome = initialIncome
        )
    }
}

@Composable
private fun WorkerGroupItemRow(
    group: com.santhomach.estateexpense.data.model.WorkerGroupEntry,
    onRemove: () -> Unit,
    onEdit: () -> Unit = {}
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
            if (group.overtimeHours > 0) {
                Text(
                    text = "OT: ${group.overtimeHours} hrs @ ₹${group.overtimeWagePerHour}/hr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
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
                text = "₹${group.calculateTotalGroupCost()}",
                style = MaterialTheme.typography.bodyMedium
            )
            Row {
                TextButton(onClick = onEdit) {
                    Text("Edit", fontSize = androidx.compose.material3.MaterialTheme.typography.labelSmall.fontSize)
                }
                TextButton(
                    onClick = onRemove,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", fontSize = androidx.compose.material3.MaterialTheme.typography.labelSmall.fontSize)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddWorkerGroupDialog(
    workerTypes: List<com.santhomach.estateexpense.data.model.WorkerType>,
    workTasks: List<com.santhomach.estateexpense.data.model.WorkTask>,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int, BigDecimal, Int, BigDecimal, String, String) -> Unit,
    onAddNewTask: (String) -> Unit,
    initialGroup: com.santhomach.estateexpense.data.model.WorkerGroupEntry? = null
) {
    var selectedWorkerTypeId by remember { mutableStateOf(initialGroup?.workerTypeId ?: workerTypes.firstOrNull()?.id ?: 0) }
    var count by remember { mutableStateOf(initialGroup?.count?.toString() ?: "1") }
    var wage by remember { 
        val defaultWage = initialGroup?.wagePerDay ?: workerTypes.find { it.id == selectedWorkerTypeId }?.dailyBasicWage ?: BigDecimal.ZERO
        mutableStateOf(defaultWage.toString()) 
    }
    var otHours by remember { mutableStateOf(initialGroup?.overtimeHours?.toString() ?: "0") }
    var otWage by remember { mutableStateOf(initialGroup?.overtimeWagePerHour?.toString() ?: "0") }
    var selectedTask by remember { 
        val taskName = initialGroup?.taskPerformed ?: workTasks.firstOrNull()?.taskName ?: ""
        mutableStateOf(taskName) 
    }
    var customTask by remember { mutableStateOf("") }
    var isCustomTask by remember { mutableStateOf(initialGroup != null && workTasks.none { it.taskName == initialGroup.taskPerformed }) }
    var comments by remember { mutableStateOf(initialGroup?.comments ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialGroup == null) "Add Worker Group" else "Edit Worker Group") },
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

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = otHours,
                        onValueChange = { if (it.all { char -> char.isDigit() }) otHours = it },
                        label = { Text("OT Hours") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = otWage,
                        onValueChange = { otWage = it },
                        label = { Text("OT Rate/hr") },
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
                        otHours.toIntOrNull() ?: 0,
                        otWage.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                        finalTask,
                        comments
                    )
                },
                enabled = (isCustomTask && customTask.isNotEmpty()) || (!isCustomTask && selectedTask.isNotEmpty())
            ) {
                Text(if (initialGroup == null) "Add" else "Update")
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
private fun ExpenseItemRow(
    expense: OtherExpenseEntry,
    onRemove: () -> Unit,
    onEdit: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = expense.typeName, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "Qty: ${expense.quantity}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
            if (expense.notes.isNotEmpty()) {
                Text(text = expense.notes, style = MaterialTheme.typography.bodySmall)
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "₹${expense.amount}", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onEdit) {
                Text("Edit", fontSize = androidx.compose.material3.MaterialTheme.typography.labelSmall.fontSize)
            }
            TextButton(
                onClick = onRemove,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.height(24.dp)
            ) {
                Text("Delete", fontSize = androidx.compose.material3.MaterialTheme.typography.labelSmall.fontSize)
            }
        }
    }
}

@Composable
private fun IncomeItemRow(
    income: com.santhomach.estateexpense.data.model.IncomeEntry,
    onRemove: () -> Unit,
    onEdit: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = income.typeName, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "${income.weight} kg @ ₹${income.pricePerKilo}/kg",
                style = MaterialTheme.typography.bodySmall
            )
            if (income.transportationCharge > BigDecimal.ZERO) {
                Text(
                    text = "Transport: ₹${income.transportationCharge}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (income.notes.isNotEmpty()) {
                Text(text = income.notes, style = MaterialTheme.typography.bodySmall)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = "₹${income.amount}", style = MaterialTheme.typography.bodyMedium)
            Row {
                TextButton(onClick = onEdit) {
                    Text("Edit", fontSize = androidx.compose.material3.MaterialTheme.typography.labelSmall.fontSize)
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExpenseDialog(
    expenseTypes: List<com.santhomach.estateexpense.data.model.ExpenseType>,
    onDismiss: () -> Unit,
    onConfirm: (Int, BigDecimal, Double, String) -> Unit,
    onAddNewType: (String) -> Unit,
    initialExpense: OtherExpenseEntry? = null
) {
    var selectedTypeId by remember { mutableStateOf(initialExpense?.expenseTypeId ?: expenseTypes.firstOrNull()?.id ?: 0) }
    var amount by remember { mutableStateOf(initialExpense?.amount?.toString() ?: "") }
    var quantity by remember { mutableStateOf(initialExpense?.quantity?.toString() ?: "1") }
    var notes by remember { mutableStateOf(initialExpense?.notes ?: "") }
    var customType by remember { mutableStateOf("") }
    var isCustomType by remember { mutableStateOf(initialExpense != null && expenseTypes.none { it.id == initialExpense.expenseTypeId }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialExpense == null) "Add Expense" else "Edit Expense") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Expense Type Dropdown
                var expanded by remember { mutableStateOf(false) }
                val selectedType = expenseTypes.find { it.id == selectedTypeId }
                val typeOptions = expenseTypes.map { it.typeName } + "Other (Enter New)"

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = if (isCustomType) "Other (Enter New)" else selectedType?.typeName ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Expense Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        typeOptions.forEach { typeName ->
                            DropdownMenuItem(
                                text = { Text(typeName) },
                                onClick = {
                                    if (typeName == "Other (Enter New)") {
                                        isCustomType = true
                                    } else {
                                        isCustomType = false
                                        selectedTypeId = expenseTypes.find { it.typeName == typeName }?.id ?: 0
                                    }
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                if (isCustomType) {
                    OutlinedTextField(
                        value = customType,
                        onValueChange = { customType = it },
                        label = { Text("Enter New Expense Type") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
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
                    if (isCustomType && customType.isNotEmpty()) {
                        onAddNewType(customType)
                        onDismiss() 
                    } else {
                        val amountValue = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
                        val qtyValue = quantity.toDoubleOrNull() ?: 1.0
                        onConfirm(selectedTypeId, amountValue, qtyValue, notes)
                    }
                },
                enabled = (isCustomType && customType.isNotEmpty()) || (!isCustomType && selectedTypeId != 0 && amount.isNotEmpty())
            ) {
                Text(if (isCustomType) "Add Type" else if (initialExpense == null) "Add" else "Update")
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
    onConfirm: (Int, BigDecimal, Double, BigDecimal, BigDecimal, String) -> Unit,
    onAddNewType: (String) -> Unit,
    initialIncome: com.santhomach.estateexpense.data.model.IncomeEntry? = null
) {
    var selectedTypeId by remember { mutableStateOf(initialIncome?.incomeTypeId ?: incomeTypes.firstOrNull()?.id ?: 0) }
    var weight by remember { mutableStateOf(initialIncome?.weight?.toString() ?: "") }
    var pricePerKilo by remember { mutableStateOf(initialIncome?.pricePerKilo?.toString() ?: "") }
    var transportCharge by remember { mutableStateOf(initialIncome?.transportationCharge?.toString() ?: "0") }
    var notes by remember { mutableStateOf(initialIncome?.notes ?: "") }
    var customType by remember { mutableStateOf("") }
    var isCustomType by remember { mutableStateOf(initialIncome != null && incomeTypes.none { it.id == initialIncome.incomeTypeId }) }

    val calculatedAmount = remember(weight, pricePerKilo, transportCharge) {
        val w = weight.toDoubleOrNull() ?: 0.0
        val p = pricePerKilo.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val t = transportCharge.toBigDecimalOrNull() ?: BigDecimal.ZERO
        (p * w.toBigDecimal()) - t
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialIncome == null) "Add Income" else "Edit Income") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Income Type Dropdown
                var expanded by remember { mutableStateOf(false) }
                val selectedType = incomeTypes.find { it.id == selectedTypeId }
                val typeOptions = incomeTypes.map { it.typeName } + "Other (Enter New)"

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = if (isCustomType) "Other (Enter New)" else selectedType?.typeName ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Income Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        typeOptions.forEach { typeName ->
                            DropdownMenuItem(
                                text = { Text(typeName) },
                                onClick = {
                                    if (typeName == "Other (Enter New)") {
                                        isCustomType = true
                                    } else {
                                        isCustomType = false
                                        selectedTypeId = incomeTypes.find { it.typeName == typeName }?.id ?: 0
                                    }
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                if (isCustomType) {
                    OutlinedTextField(
                        value = customType,
                        onValueChange = { customType = it },
                        label = { Text("Enter New Income Type") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it },
                        label = { Text("Weight (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = pricePerKilo,
                        onValueChange = { pricePerKilo = it },
                        label = { Text("Price/kg") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = transportCharge,
                    onValueChange = { transportCharge = it },
                    label = { Text("Transportation Charge (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Divider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Net Income:", style = MaterialTheme.typography.bodyMedium)
                    Text("₹$calculatedAmount", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
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
                    if (isCustomType && customType.isNotEmpty()) {
                        onAddNewType(customType)
                        onDismiss()
                    } else {
                        onConfirm(
                            selectedTypeId,
                            calculatedAmount,
                            weight.toDoubleOrNull() ?: 0.0,
                            pricePerKilo.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                            transportCharge.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                            notes
                        )
                    }
                },
                enabled = (isCustomType && customType.isNotEmpty()) || (!isCustomType && selectedTypeId != 0 && weight.isNotEmpty() && pricePerKilo.isNotEmpty())
            ) {
                Text(if (isCustomType) "Add Type" else if (initialIncome == null) "Add" else "Update")
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
private fun SummaryRow(
    label: String,
    value: String,
    isTotal: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (isTotal) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = if (isTotal) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium
        )
    }
}
