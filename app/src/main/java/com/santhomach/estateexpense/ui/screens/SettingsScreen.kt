package com.santhomach.estateexpense.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Save
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.santhomach.estateexpense.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import java.math.BigDecimal

import androidx.compose.ui.platform.LocalInspectionMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToCsvImport: () -> Unit = {},
    viewModelArg: SettingsViewModel? = null
) {
    if (LocalInspectionMode.current && viewModelArg == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Settings Screen Preview")
        }
        return
    }

    val viewModel: SettingsViewModel = viewModelArg ?: hiltViewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            scope.launch {
                viewModel.importData(it, context)
            }
        }
    }

    val workers by viewModel.permanentWorkers.collectAsState()
    val workerTypes by viewModel.workerTypes.collectAsState()
    val expenseTypes by viewModel.expenseTypes.collectAsState()
    val expenseSubtypes by viewModel.expenseSubtypes.collectAsState()
    val incomeTypes by viewModel.incomeTypes.collectAsState()
    val workTasks by viewModel.workTasks.collectAsState()

    var showAddWorkerDialog by remember { mutableStateOf(false) }
    var showAddWorkerTypeDialog by remember { mutableStateOf(false) }
    var showAddExpenseTypeDialog by remember { mutableStateOf(false) }
    var showAddIncomeTypeDialog by remember { mutableStateOf(false) }
    var showAddWorkTaskDialog by remember { mutableStateOf(false) }

    // State for editing items
    var editingWorker by remember { mutableStateOf<com.santhomach.estateexpense.data.model.PermanentWorker?>(null) }
    var editingWorkerType by remember { mutableStateOf<com.santhomach.estateexpense.data.model.WorkerType?>(null) }
    var editingExpenseType by remember { mutableStateOf<com.santhomach.estateexpense.data.model.ExpenseType?>(null) }
    var editingIncomeType by remember { mutableStateOf<com.santhomach.estateexpense.data.model.IncomeType?>(null) }
    var editingWorkTask by remember { mutableStateOf<com.santhomach.estateexpense.data.model.WorkTask?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Settings are saved automatically */ }) {
                        Icon(Icons.Filled.Save, contentDescription = "Save")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Manage Staff Section
            item {
                SettingsCategoryCard(
                    title = "Manage Managers / Staff",
                    items = workers.map { "${it.name} (${it.role})" },
                    onAddClick = { showAddWorkerDialog = true },
                    onEditClick = { index -> editingWorker = workers[index] },
                    onDeleteClick = { index -> viewModel.deletePermanentWorker(workers[index]) }
                )
            }

            // Worker Types Section
            item {
                SettingsCategoryCard(
                    title = "Worker Types & Wages",
                    items = workerTypes.map { "${it.workerTypeName}: ₹${it.dailyBasicWage}" },
                    onAddClick = { showAddWorkerTypeDialog = true },
                    onEditClick = { index -> editingWorkerType = workerTypes[index] },
                    onDeleteClick = { index -> viewModel.deleteWorkerType(workerTypes[index]) }
                )
            }

            // Work Tasks Section
            item {
                SettingsCategoryCard(
                    title = "Work Tasks",
                    items = workTasks.map { it.taskName },
                    onAddClick = { showAddWorkTaskDialog = true },
                    onEditClick = { index -> editingWorkTask = workTasks[index] },
                    onDeleteClick = { index -> viewModel.deleteWorkTask(workTasks[index]) }
                )
            }

            // Expense Types Section
            item {
                SettingsCategoryCard(
                    title = "Expense Categories",
                    items = expenseTypes.map { it.typeName },
                    onAddClick = { showAddExpenseTypeDialog = true },
                    onEditClick = { index -> editingExpenseType = expenseTypes[index] },
                    onDeleteClick = { index -> viewModel.deleteExpenseType(expenseTypes[index]) }
                )
            }

            // Expense Subtypes Section
            item {
                SettingsCategoryCard(
                    title = "Expense Sub-categories",
                    items = expenseSubtypes.map { "${it.typeName} (${it.parentTypeName.typeName})" },
                    onAddClick = { /* We usually add these via the expense entry, but let's allow deletion here */ },
                    onDeleteClick = { index -> viewModel.deleteExpenseSubtype(expenseSubtypes[index]) }
                )
            }

            // Income Types Section
            item {
                SettingsCategoryCard(
                    title = "Income Categories",
                    items = incomeTypes.map { it.typeName },
                    onAddClick = { showAddIncomeTypeDialog = true },
                    onEditClick = { index -> editingIncomeType = incomeTypes[index] },
                    onDeleteClick = { index -> viewModel.deleteIncomeType(incomeTypes[index]) }
                )
            }

            // Data Management Section
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Data Management",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    viewModel.exportData(context)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isExporting
                        ) {
                            Text(if (uiState.isExporting) "Exporting..." else "Export Data to JSON")
                        }

                        if (uiState.exportMessage != null) {
                            Text(
                                text = uiState.exportMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { importLauncher.launch("application/json") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isExporting
                        ) {
                            Text(if (uiState.isExporting) "Importing..." else "Import Data from JSON")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = onNavigateToCsvImport,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Import from CSV (Bulk Backlog)")
                        }
                    }
                }
            }

            // About Section
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "About",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Text(
                            text = "Estate Expense Tracker v1.0",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Text(
                            text = "Track daily expenses and income for your cardamom/pepper plantation operations.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // Error handling
            uiState.error?.let { error ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
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

    if (showAddWorkerDialog || editingWorker != null) {
        AddWorkerDialog(
            initialWorker = editingWorker,
            onDismiss = { 
                showAddWorkerDialog = false
                editingWorker = null
            },
            onConfirm = { name, role, wage ->
                if (editingWorker != null) {
                    viewModel.updatePermanentWorker(editingWorker!!.copy(name = name, role = role, dailyBasicWage = wage))
                } else {
                    viewModel.addPermanentWorker(name, role, wage)
                }
                showAddWorkerDialog = false
                editingWorker = null
            }
        )
    }

    if (showAddWorkerTypeDialog || editingWorkerType != null) {
        AddSimpleItemDialog(
            title = if (editingWorkerType == null) "Add Worker Type" else "Edit Worker Type",
            label = "Worker Type Name",
            hasWage = true,
            initialName = editingWorkerType?.workerTypeName ?: "",
            initialWage = editingWorkerType?.dailyBasicWage?.toString() ?: "0",
            onDismiss = { 
                showAddWorkerTypeDialog = false
                editingWorkerType = null
            },
            onConfirm = { name, wage ->
                if (editingWorkerType != null) {
                    viewModel.updateWorkerType(editingWorkerType!!.copy(workerTypeName = name, dailyBasicWage = wage))
                } else {
                    viewModel.addWorkerType(name, wage)
                }
                showAddWorkerTypeDialog = false
                editingWorkerType = null
            }
        )
    }

    if (showAddExpenseTypeDialog || editingExpenseType != null) {
        AddSimpleItemDialog(
            title = if (editingExpenseType == null) "Add Expense Category" else "Edit Expense Category",
            label = "Category Name",
            initialName = editingExpenseType?.typeName ?: "",
            onDismiss = { 
                showAddExpenseTypeDialog = false
                editingExpenseType = null
            },
            onConfirm = { name, _ ->
                if (editingExpenseType != null) {
                    viewModel.updateExpenseType(editingExpenseType!!.copy(typeName = name))
                } else {
                    viewModel.addExpenseType(name)
                }
                showAddExpenseTypeDialog = false
                editingExpenseType = null
            }
        )
    }

    if (showAddIncomeTypeDialog || editingIncomeType != null) {
        AddSimpleItemDialog(
            title = if (editingIncomeType == null) "Add Income Category" else "Edit Income Category",
            label = "Category Name",
            initialName = editingIncomeType?.typeName ?: "",
            onDismiss = { 
                showAddIncomeTypeDialog = false
                editingIncomeType = null
            },
            onConfirm = { name, _ ->
                if (editingIncomeType != null) {
                    viewModel.updateIncomeType(editingIncomeType!!.copy(typeName = name))
                } else {
                    viewModel.addIncomeType(name)
                }
                showAddIncomeTypeDialog = false
                editingIncomeType = null
            }
        )
    }

    if (showAddWorkTaskDialog || editingWorkTask != null) {
        AddSimpleItemDialog(
            title = if (editingWorkTask == null) "Add Work Task" else "Edit Work Task",
            label = "Task Name",
            initialName = editingWorkTask?.taskName ?: "",
            onDismiss = { 
                showAddWorkTaskDialog = false
                editingWorkTask = null
            },
            onConfirm = { name, _ ->
                if (editingWorkTask != null) {
                    viewModel.updateWorkTask(editingWorkTask!!.copy(taskName = name))
                } else {
                    viewModel.addWorkTask(name)
                }
                showAddWorkTaskDialog = false
                editingWorkTask = null
            }
        )
    }
}

@Composable
fun SettingsCategoryCard(
    title: String,
    items: List<String>,
    onAddClick: () -> Unit,
    onEditClick: (Int) -> Unit = {},
    onDeleteClick: (Int) -> Unit = {}
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onAddClick) {
                    Icon(Icons.Filled.Add, contentDescription = "Add")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (items.isEmpty()) {
                Text(
                    text = "None configured",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                items.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Row {
                            TextButton(onClick = { onEditClick(index) }) {
                                Text("Edit", fontSize = MaterialTheme.typography.labelSmall.fontSize)
                            }
                            TextButton(
                                onClick = { onDeleteClick(index) },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Delete", fontSize = MaterialTheme.typography.labelSmall.fontSize)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWorkerDialog(
    initialWorker: com.santhomach.estateexpense.data.model.PermanentWorker? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String, BigDecimal) -> Unit
) {
    var name by remember { mutableStateOf(initialWorker?.name ?: "") }
    var role by remember { mutableStateOf(initialWorker?.role ?: "Manager") }
    var wage by remember { mutableStateOf(initialWorker?.dailyBasicWage?.toString() ?: "0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialWorker == null) "Add Manager / Permanent Worker" else "Edit Manager / Permanent Worker") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                var expanded by remember { mutableStateOf(false) }
                val roles = listOf("Manager", "Permanent Labor", "Specialist")
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = role,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Role") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        roles.forEach { r ->
                            DropdownMenuItem(
                                text = { Text(r) },
                                onClick = {
                                    role = r
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = wage,
                    onValueChange = { wage = it },
                    label = { Text("Daily/Basic Wage (₹)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, role, wage.toBigDecimalOrNull() ?: BigDecimal.ZERO) },
                enabled = name.isNotEmpty()
            ) {
                Text(if (initialWorker == null) "Add" else "Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSimpleItemDialog(
    title: String,
    label: String,
    hasWage: Boolean = false,
    initialName: String = "",
    initialWage: String = "0",
    onDismiss: () -> Unit,
    onConfirm: (String, BigDecimal) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var wage by remember { mutableStateOf(initialWage) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(label) },
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (hasWage) {
                    OutlinedTextField(
                        value = wage,
                        onValueChange = { wage = it },
                        label = { Text("Basic Daily Wage (₹)") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, wage.toBigDecimalOrNull() ?: BigDecimal.ZERO) },
                enabled = name.isNotEmpty()
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
