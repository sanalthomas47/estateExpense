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
import com.santhomach.estateexpense.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import java.math.BigDecimal

import androidx.compose.ui.platform.LocalInspectionMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
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
    val workers by viewModel.permanentWorkers.collectAsState()
    val workerTypes by viewModel.workerTypes.collectAsState()
    val expenseTypes by viewModel.expenseTypes.collectAsState()
    val incomeTypes by viewModel.incomeTypes.collectAsState()
    val workTasks by viewModel.workTasks.collectAsState()

    var showAddWorkerDialog by remember { mutableStateOf(false) }
    var showAddWorkerTypeDialog by remember { mutableStateOf(false) }
    var showAddExpenseTypeDialog by remember { mutableStateOf(false) }
    var showAddIncomeTypeDialog by remember { mutableStateOf(false) }
    var showAddWorkTaskDialog by remember { mutableStateOf(false) }

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
                    onAddClick = { showAddWorkerDialog = true }
                )
            }

            // Worker Types Section
            item {
                SettingsCategoryCard(
                    title = "Worker Types & Wages",
                    items = workerTypes.map { "${it.workerTypeName}: ₹${it.dailyBasicWage}" },
                    onAddClick = { showAddWorkerTypeDialog = true }
                )
            }

            // Work Tasks Section
            item {
                SettingsCategoryCard(
                    title = "Work Tasks",
                    items = workTasks.map { it.taskName },
                    onAddClick = { showAddWorkTaskDialog = true }
                )
            }

            // Expense Types Section
            item {
                SettingsCategoryCard(
                    title = "Expense Categories",
                    items = expenseTypes.map { it.typeName },
                    onAddClick = { showAddExpenseTypeDialog = true }
                )
            }

            // Income Types Section
            item {
                SettingsCategoryCard(
                    title = "Income Categories",
                    items = incomeTypes.map { it.typeName },
                    onAddClick = { showAddIncomeTypeDialog = true }
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
                            onClick = { /* TODO: Implement import */ },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Import Data from JSON")
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

    if (showAddWorkerDialog) {
        AddWorkerDialog(
            onDismiss = { showAddWorkerDialog = false },
            onConfirm = { name, role, wage ->
                viewModel.addPermanentWorker(name, role, wage)
                showAddWorkerDialog = false
            }
        )
    }

    if (showAddWorkerTypeDialog) {
        AddSimpleItemDialog(
            title = "Add Worker Type",
            label = "Worker Type Name",
            hasWage = true,
            onDismiss = { showAddWorkerTypeDialog = false },
            onConfirm = { name, wage ->
                viewModel.addWorkerType(name, wage)
                showAddWorkerTypeDialog = false
            }
        )
    }

    if (showAddExpenseTypeDialog) {
        AddSimpleItemDialog(
            title = "Add Expense Category",
            label = "Category Name",
            onDismiss = { showAddExpenseTypeDialog = false },
            onConfirm = { name, _ ->
                viewModel.addExpenseType(name)
                showAddExpenseTypeDialog = false
            }
        )
    }

    if (showAddIncomeTypeDialog) {
        AddSimpleItemDialog(
            title = "Add Income Category",
            label = "Category Name",
            onDismiss = { showAddIncomeTypeDialog = false },
            onConfirm = { name, _ ->
                viewModel.addIncomeType(name)
                showAddIncomeTypeDialog = false
            }
        )
    }

    if (showAddWorkTaskDialog) {
        AddSimpleItemDialog(
            title = "Add Work Task",
            label = "Task Name",
            onDismiss = { showAddWorkTaskDialog = false },
            onConfirm = { name, _ ->
                viewModel.addWorkTask(name)
                showAddWorkTaskDialog = false
            }
        )
    }
}

@Composable
fun SettingsCategoryCard(
    title: String,
    items: List<String>,
    onAddClick: () -> Unit
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
                items.forEach { item ->
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWorkerDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, BigDecimal) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("Manager") }
    var wage by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Manager / Permanent Worker") },
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
                Text("Add")
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
    onDismiss: () -> Unit,
    onConfirm: (String, BigDecimal) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var wage by remember { mutableStateOf("0") }

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
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
