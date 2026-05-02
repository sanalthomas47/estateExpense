package com.santhomach.estateexpense.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.santhomach.estateexpense.data.model.OtherExpenseEntry
import androidx.compose.foundation.shape.RoundedCornerShape
import com.santhomach.estateexpense.ui.viewmodel.DailyExpenseViewModel
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import androidx.compose.ui.platform.LocalInspectionMode
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import java.io.File
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip

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
    val expenseSubtypes by viewModel.expenseSubtypes.collectAsState()
    val incomeTypes by viewModel.incomeTypes.collectAsState()
    val workerTypes by viewModel.workerTypes.collectAsState()
    val permanentWorkers by viewModel.permanentWorkers.collectAsState()
    val workTasks by viewModel.workTasks.collectAsState()

    val previousExcessBalance by viewModel.previousExcessBalance.collectAsState()

    val scrollState = rememberScrollState()

    var shouldNavigateBack by remember { mutableStateOf(false) }

    // State for editing items
    var editingWorkerGroupIndex by remember { mutableStateOf<Int?>(null) }
    var showAddWorkerGroupDialog by remember { mutableStateOf(false) }

    var editingOtherExpenseIndex by remember { mutableStateOf<Int?>(null) }
    var showAddOtherExpenseDialog by remember { mutableStateOf(false) }

    var editingIncomeIndex by remember { mutableStateOf<Int?>(null) }
    var showAddIncomeDialog by remember { mutableStateOf(false) }

    var editingAdvanceIndex by remember { mutableStateOf<Int?>(null) }
    var showAddAdvanceDialog by remember { mutableStateOf(false) }

    val workerGroups = remember(currentExpense) {
        viewModel.getWorkerGroups()
    }

    val otherExpenses = remember(currentExpense) {
        viewModel.getOtherExpenses()
    }

    val incomeEntries = remember(currentExpense) {
        viewModel.getIncomeEntries()
    }

    val advanceEntries = remember(currentExpense) {
        viewModel.getAdvanceEntries()
    }

    // Load expense if editing
    LaunchedEffect(expenseId, date) {
        if (expenseId != null) {
            viewModel.loadExpense(expenseId)
        } else {
            viewModel.loadOrCreateExpense(date)
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
                title = { Text(if (expenseId != null || (currentExpense != null && currentExpense!!.id != 0)) "Edit Expense" else "Add Daily Expense") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val isExistingExpense = expenseId != null || (currentExpense != null && currentExpense!!.id != 0)
                    if (isExistingExpense) {
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
            Card(modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
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
                        value = previousExcessBalance.toString(),
                        onValueChange = { /* Read-only */ },
                        label = { Text("Previous Excess Balance (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        enabled = false
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
            Card(modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Worker Groups & Tasks",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

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
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
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
            Card(modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Extra Overtime",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

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
            Card(modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Other Expenses",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

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
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
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
            Card(modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                Column(modifier = Modifier.padding(16.dp)) {

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Income",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

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
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Advance Payments",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    advanceEntries.forEachIndexed { index, advance ->
                        AdvanceItemRow(
                            advance = advance,
                            onRemove = { viewModel.removeAdvanceEntry(index) },
                            onEdit = { editingAdvanceIndex = index }
                        )
                        if (index < advanceEntries.size - 1) {
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }

                    if (advanceEntries.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    OutlinedButton(
                        onClick = { showAddAdvanceDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Advance")
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        text = "Total Advance: ₹${currentExpense?.advanceAmount ?: BigDecimal.ZERO}",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }

            // Adjustments Section
            Card(modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Weekly Settlement",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = (currentExpense?.weeklyPaymentDone ?: BigDecimal.ZERO).toString(),
                        onValueChange = { viewModel.updateWeeklyPaymentDone(it.toBigDecimalOrNull() ?: BigDecimal.ZERO) },
                        label = { Text("Payment Done for the Week (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        prefix = { Text("₹") },
                        supportingText = { Text("Amount paid to manager/workers for the full week") }
                    )
                }
            }

            // Comments Section
            Card(modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
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
                    SummaryRow("Other Expenses", "₹${currentExpense?.totalOtherExpensesCost ?: BigDecimal.ZERO}")
                    
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    SummaryRow("TOTAL ACTUAL COSTS", "₹${viewModel.getTotalActualExpenses()}", isTotal = true)
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Payments & Offset", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    
                    SummaryRow("Previous Excess/Deficit", "₹$previousExcessBalance")
                    SummaryRow("Total Advances Paid", "₹${currentExpense?.advanceAmount ?: BigDecimal.ZERO}")
                    SummaryRow("Weekly Settlement Done", "₹${currentExpense?.weeklyPaymentDone ?: BigDecimal.ZERO}")
                    
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    val totalPayments = viewModel.getTotalPaymentsMade()
                    SummaryRow("TOTAL PAYMENTS", "₹$totalPayments", isTotal = true)
                    
                    val netRemaining = viewModel.getNetAmount()
                    SummaryRow(
                        label = if (netRemaining >= BigDecimal.ZERO) "REMAINING BALANCE (Excess)" else "REMAINING DEFICIT",
                        value = "₹${netRemaining.abs()}",
                        isTotal = true
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp))
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
            expenseSubtypes = expenseSubtypes,
            onDismiss = { 
                showAddOtherExpenseDialog = false
                editingOtherExpenseIndex = null
            },
            onConfirm = { typeId, customName,customSubtype, amount, quantity, notes, imagePath ->
                if (editingOtherExpenseIndex != null) {
                    viewModel.updateOtherExpense(editingOtherExpenseIndex!!, typeId, amount, quantity, notes, customName,customSubtype, imagePath)
                } else {
                    viewModel.addOtherExpense(typeId, amount, quantity, notes, customName,customSubtype, imagePath)
                }
                showAddOtherExpenseDialog = false
                editingOtherExpenseIndex = null
            },
            initialExpense = initialExpense,
            onSaveImage = { uri -> viewModel.saveReceiptImage(uri) }
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
            onConfirm = { typeId, customName, amount, weight, price, transport, notes ->
                if (editingIncomeIndex != null) {
                    viewModel.updateIncome(editingIncomeIndex!!, typeId, amount, weight, price, transport, notes, customName)
                } else {
                    viewModel.addIncome(typeId, amount, weight, price, transport, notes, customName)
                }
                showAddIncomeDialog = false
                editingIncomeIndex = null
            },
            initialIncome = initialIncome
        )
    }

    if (showAddAdvanceDialog || editingAdvanceIndex != null) {
        val initialAdvance = editingAdvanceIndex?.let { advanceEntries.getOrNull(it) }
        AddAdvanceDialog(
            onDismiss = {
                showAddAdvanceDialog = false
                editingAdvanceIndex = null
            },
            onConfirm = { amount, reason, recipient ->
                if (editingAdvanceIndex != null) {
                    viewModel.updateAdvanceEntry(editingAdvanceIndex!!, amount, reason, recipient)
                } else {
                    viewModel.addAdvanceEntry(amount, reason, recipient)
                }
                showAddAdvanceDialog = false
                editingAdvanceIndex = null
            },
            initialAdvance = initialAdvance
        )
    }
}

@Composable
private fun AdvanceItemRow(
    advance: com.santhomach.estateexpense.data.model.AdvanceEntry,
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
                text = if (advance.recipientName.isNotEmpty()) "To: ${advance.recipientName}" else "Advance",
                style = MaterialTheme.typography.bodyMedium
            )
            if (advance.reason.isNotEmpty()) {
                Text(
                    text = advance.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = "₹${advance.amount}", style = MaterialTheme.typography.bodyMedium)
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAdvanceDialog(
    onDismiss: () -> Unit,
    onConfirm: (BigDecimal, String, String) -> Unit,
    initialAdvance: com.santhomach.estateexpense.data.model.AdvanceEntry? = null
) {
    var amount by remember { mutableStateOf(initialAdvance?.amount?.toString() ?: "") }
    var reason by remember { mutableStateOf(initialAdvance?.reason ?: "") }
    var recipient by remember { mutableStateOf(initialAdvance?.recipientName ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialAdvance == null) "Add Advance Payment" else "Edit Advance Payment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = recipient,
                    onValueChange = { recipient = it },
                    label = { Text("Recipient Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason / Note") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountValue = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    onConfirm(amountValue, reason, recipient)
                },
                enabled = amount.isNotEmpty()
            ) {
                Text(if (initialAdvance == null) "Add" else "Update")
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "₹${group.calculateTotalGroupCost()}",
                style = MaterialTheme.typography.bodyMedium
            )
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Edit",
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
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
    // Internal row state — kept private to this composable
    data class RowState(
        val id: Int = System.nanoTime().toInt(),
        val workerTypeId: Int,
        val count: String,
        val wage: String,
        val otHours: String,
        val otWage: String
    )

    var rows by remember {
        mutableStateOf(
            if (initialGroup != null) listOf(
                RowState(
                    workerTypeId = initialGroup.workerTypeId,
                    count = initialGroup.count.toString(),
                    wage = initialGroup.wagePerDay.toString(),
                    otHours = initialGroup.overtimeHours.toString(),
                    otWage = initialGroup.overtimeWagePerHour.toString()
                )
            ) else listOf(
                RowState(
                    workerTypeId = workerTypes.firstOrNull()?.id ?: 0,
                    count = "1",
                    wage = workerTypes.firstOrNull()?.dailyBasicWage?.toString() ?: "0",
                    otHours = "0",
                    otWage = "0"
                )
            )
        )
    }

    var selectedTask by remember {
        mutableStateOf(initialGroup?.taskPerformed ?: workTasks.firstOrNull()?.taskName ?: "")
    }
    var customTask by remember { mutableStateOf("") }
    var isCustomTask by remember {
        mutableStateOf(initialGroup != null && workTasks.none { it.taskName == initialGroup.taskPerformed })
    }
    var comments by remember { mutableStateOf(initialGroup?.comments ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialGroup == null) "Add Worker Groups" else "Edit Worker Group") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Worker rows ──────────────────────────────────────────
                rows.forEachIndexed { index, row ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Worker Type + Delete button
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                var workerExpanded by remember { mutableStateOf(false) }
                                val selectedWorkerType = workerTypes.find { it.id == row.workerTypeId }

                                ExposedDropdownMenuBox(
                                    expanded = workerExpanded,
                                    onExpandedChange = { workerExpanded = it },
                                    modifier = Modifier.weight(1f)
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
                                                    rows = rows.toMutableList().also {
                                                        it[index] = row.copy(
                                                            workerTypeId = type.id,
                                                            wage = type.dailyBasicWage.toString()
                                                        )
                                                    }
                                                    workerExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                if (rows.size > 1) {
                                    IconButton(onClick = {
                                        rows = rows.toMutableList().also { it.removeAt(index) }
                                    }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Remove row",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }

                            // Count + Daily Wage
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = row.count,
                                    onValueChange = { v ->
                                        if (v.all { it.isDigit() })
                                            rows = rows.toMutableList().also { it[index] = row.copy(count = v) }
                                    },
                                    label = { Text("Count") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = row.wage,
                                    onValueChange = { v ->
                                        rows = rows.toMutableList().also { it[index] = row.copy(wage = v) }
                                    },
                                    label = { Text("Daily Wage") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1.5f)
                                )
                            }

                            // OT Hours + OT Rate
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = row.otHours,
                                    onValueChange = { v ->
                                        if (v.all { it.isDigit() })
                                            rows = rows.toMutableList().also { it[index] = row.copy(otHours = v) }
                                    },
                                    label = { Text("OT Hours") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = row.otWage,
                                    onValueChange = { v ->
                                        rows = rows.toMutableList().also { it[index] = row.copy(otWage = v) }
                                    },
                                    label = { Text("OT Rate/hr") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1.5f)
                                )
                            }
                        }
                    }
                }

                // ── Add row button ───────────────────────────────────────
                OutlinedButton(
                    onClick = {
                        rows = rows + RowState(
                            workerTypeId = workerTypes.firstOrNull()?.id ?: 0,
                            count = "1",
                            wage = workerTypes.firstOrNull()?.dailyBasicWage?.toString() ?: "0",
                            otHours = "0",
                            otWage = "0"
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Add Worker Type")
                }

                HorizontalDivider()

                // ── Shared Task dropdown ─────────────────────────────────
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
                                    isCustomTask = task == "Other (Enter New)"
                                    if (!isCustomTask) selectedTask = task
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
            val finalTask = if (isCustomTask) customTask else selectedTask
            val isValid = ((isCustomTask && customTask.isNotEmpty()) || (!isCustomTask && selectedTask.isNotEmpty()))
                    && rows.all { it.count.toIntOrNull() != null && it.wage.toBigDecimalOrNull() != null }

            Button(
                onClick = {
                    if (isCustomTask && customTask.isNotEmpty()) onAddNewTask(customTask)
                    // Call onConfirm once per row — signature unchanged
                    rows.forEach { row ->
                        onConfirm(
                            row.workerTypeId,
                            row.count.toIntOrNull() ?: 0,
                            row.wage.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                            row.otHours.toIntOrNull() ?: 0,
                            row.otWage.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                            finalTask,
                            comments
                        )
                    }
                },
                enabled = isValid
            ) {
                Text(if (initialGroup == null) "Add" else "Update")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
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
        if (expense.receiptImagePath != null) {
            AsyncImage(
                model = File(expense.receiptImagePath),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            val titleText = if (expense.subtypeName != null) {
                "${expense.typeName} (${expense.subtypeName})"
            } else {
                expense.typeName
            }
            Text(text = titleText, style = MaterialTheme.typography.bodyMedium)
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = "₹${expense.amount}", style = MaterialTheme.typography.bodyMedium)
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Edit",
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = "₹${income.amount}", style = MaterialTheme.typography.bodyMedium)
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Edit",
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExpenseDialog(
    expenseTypes: List<com.santhomach.estateexpense.data.model.ExpenseType>,
    expenseSubtypes: List<com.santhomach.estateexpense.data.model.ExpenseSubtype>,
    onDismiss: () -> Unit,
    onConfirm: (Int, String?, String?, BigDecimal, Double, String, String?) -> Unit,
    initialExpense: OtherExpenseEntry? = null,
    onSaveImage: (android.net.Uri) -> String? = { null }
) {
    var selectedTypeId by remember { mutableIntStateOf(initialExpense?.expenseTypeId ?: expenseTypes.firstOrNull()?.id ?: 0) }
    var selectedSubtypeName by remember { mutableStateOf(initialExpense?.subtypeName ?: "") }
    var amount by remember { mutableStateOf(initialExpense?.amount?.toString() ?: "") }
    var quantity by remember { mutableStateOf(initialExpense?.quantity?.toString() ?: "1") }
    var notes by remember { mutableStateOf(initialExpense?.notes ?: "") }
    var customType by remember { mutableStateOf("") }
    var customSubtype by remember { mutableStateOf("") }
    var isCustomType by remember { mutableStateOf(false) }
    var isCustomSubtype by remember { mutableStateOf(false) }
    var receiptImagePath by remember { mutableStateOf(initialExpense?.receiptImagePath) }

    LaunchedEffect(initialExpense, expenseSubtypes) {
        if (initialExpense != null) {
            selectedTypeId = initialExpense.expenseTypeId
            selectedSubtypeName = initialExpense.subtypeName ?: ""
            amount = initialExpense.amount.toString()
            quantity = initialExpense.quantity.toString()
            notes = initialExpense.notes
            receiptImagePath = initialExpense.receiptImagePath
            isCustomSubtype = initialExpense.subtypeName != null && 
                expenseSubtypes.none { it.typeName == initialExpense.subtypeName && it.parentTypeName.id == initialExpense.expenseTypeId }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            receiptImagePath = onSaveImage(it)
        }
    }

    val filteredSubtypes = remember(selectedTypeId, expenseSubtypes, expenseTypes) {
        val selectedType = expenseTypes.find { it.id == selectedTypeId }
        if (selectedType != null) {
            expenseSubtypes.filter { it.parentTypeName.typeName == selectedType.typeName }
        } else {
            emptyList()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialExpense == null) "Add Expense" else "Edit Expense") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Receipt Image Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (receiptImagePath != null) {
                        AsyncImage(
                            model = File(receiptImagePath!!),
                            contentDescription = "Receipt",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { receiptImagePath = null },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove Image", tint = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(48.dp))
                            Text("Add Receipt Image", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // Expense Type Dropdown
                var typeExpanded by remember { mutableStateOf(false) }
                val selectedType = expenseTypes.find { it.id == selectedTypeId }
                val typeOptions = expenseTypes.map { it.typeName } + "Other (Enter New)"

                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = if (isCustomType) "Other (Enter New)" else selectedType?.typeName ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Expense Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        typeOptions.forEach { typeName ->
                            DropdownMenuItem(
                                text = { Text(typeName) },
                                onClick = {
                                    if (typeName == "Other (Enter New)") {
                                        isCustomType = true
                                        selectedTypeId = 0
                                    } else {
                                        isCustomType = false
                                        selectedTypeId = expenseTypes.find { it.typeName == typeName }?.id ?: 0
                                    }
                                    selectedSubtypeName = ""
                                    isCustomSubtype = false
                                    typeExpanded = false
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

                // Expense Subtype Dropdown (only show if not custom type or if custom type entered)
                if (!isCustomType || customType.isNotEmpty()) {
                    var subtypeExpanded by remember { mutableStateOf(false) }
                    val subtypeOptions = filteredSubtypes.map { it.typeName } + "Other (Enter New)"

                    ExposedDropdownMenuBox(
                        expanded = subtypeExpanded,
                        onExpandedChange = { subtypeExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = if (isCustomSubtype) "Other (Enter New)" else selectedSubtypeName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Expense Subtype (Optional)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subtypeExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = subtypeExpanded,
                            onDismissRequest = { subtypeExpanded = false }
                        ) {
                            subtypeOptions.forEach { subtypeName ->
                                DropdownMenuItem(
                                    text = { Text(subtypeName) },
                                    onClick = {
                                        if (subtypeName == "Other (Enter New)") {
                                            isCustomSubtype = true
                                        } else {
                                            isCustomSubtype = false
                                            selectedSubtypeName = subtypeName
                                        }
                                        subtypeExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (isCustomSubtype) {
                        OutlinedTextField(
                            value = customSubtype,
                            onValueChange = { customSubtype = it },
                            label = { Text("Enter New Expense Subtype") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
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
                    val amountValue = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    val qtyValue = quantity.toDoubleOrNull() ?: 1.0
                    val finalSubtype = if (isCustomSubtype) customSubtype else if (selectedSubtypeName.isNotEmpty()) selectedSubtypeName else null
                    
                    if (isCustomType && customType.isNotEmpty()) {
                        onConfirm(0, customType, finalSubtype, amountValue, qtyValue, notes, receiptImagePath)
                    } else {
                        onConfirm(selectedTypeId, null, finalSubtype, amountValue, qtyValue, notes, receiptImagePath)
                    }
                },
                enabled = ((isCustomType && customType.isNotEmpty()) || (!isCustomType && selectedTypeId != 0)) && amount.isNotEmpty()
            ) {
                Text(if (initialExpense == null) "Add" else "Update")
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
    onConfirm: (Int, String?, BigDecimal, Double, BigDecimal, BigDecimal, String) -> Unit,
    initialIncome: com.santhomach.estateexpense.data.model.IncomeEntry? = null
) {
    var selectedTypeId by remember { mutableStateOf(initialIncome?.incomeTypeId ?: incomeTypes.firstOrNull()?.id ?: 0) }
    var weight by remember { mutableStateOf(initialIncome?.weight?.toString() ?: "") }
    var pricePerKilo by remember { mutableStateOf(initialIncome?.pricePerKilo?.toString() ?: "") }
    var transportCharge by remember { mutableStateOf(initialIncome?.transportationCharge?.toString() ?: "0") }
    var notes by remember { mutableStateOf(initialIncome?.notes ?: "") }
    var customType by remember { mutableStateOf("") }
    var isCustomType by remember { mutableStateOf(false) }

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
                        onConfirm(
                            0,
                            customType,
                            calculatedAmount,
                            weight.toDoubleOrNull() ?: 0.0,
                            pricePerKilo.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                            transportCharge.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                            notes
                        )
                    } else {
                        onConfirm(
                            selectedTypeId,
                            null,
                            calculatedAmount,
                            weight.toDoubleOrNull() ?: 0.0,
                            pricePerKilo.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                            transportCharge.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                            notes
                        )
                    }
                },
                enabled = (isCustomType && customType.isNotEmpty() && weight.isNotEmpty() && pricePerKilo.isNotEmpty()) || (!isCustomType && selectedTypeId != 0 && weight.isNotEmpty() && pricePerKilo.isNotEmpty())
            ) {
                Text(if (initialIncome == null) "Add" else "Update")
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
