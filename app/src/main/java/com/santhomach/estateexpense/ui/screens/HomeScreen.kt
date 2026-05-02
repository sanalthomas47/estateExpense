package com.santhomach.estateexpense.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.santhomach.estateexpense.ui.viewmodel.ReportsViewModel
import androidx.compose.ui.platform.LocalInspectionMode
import com.santhomach.estateexpense.data.model.*
import com.santhomach.estateexpense.data.repository.ExpenseSummary
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.serialization.builtins.ListSerializer
import java.io.File
import coil.compose.AsyncImage
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToExpenseEntry: (LocalDate, Int?) -> Unit = { _, _ -> },
    onNavigateToReports: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToPayments: () -> Unit = {},
    onNavigateToWeeklyFunds: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    viewModel: ReportsViewModel? = null
) {
    if (LocalInspectionMode.current && viewModel == null) {
        HomeScreenContent(
            recentExpenses = emptyList(),
            dailySummary = ExpenseSummary(),
            onNavigateToExpenseEntry = onNavigateToExpenseEntry,
            onNavigateToReports = onNavigateToReports,
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToPayments = onNavigateToPayments,
            onNavigateToWeeklyFunds = onNavigateToWeeklyFunds,
            onNavigateToSearch = onNavigateToSearch
        )
        return
    }

    val actualViewModel: ReportsViewModel = viewModel ?: hiltViewModel()
    val recentExpenses by actualViewModel.recentExpenses.collectAsState()
    val dailySummary by actualViewModel.dailySummary.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }
    var selectedExpenseForView by remember { mutableStateOf<DailyExpense?>(null) }
    var selectedImagePathForPreview by remember { mutableStateOf<String?>(null) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selectedDate = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                        onNavigateToExpenseEntry(selectedDate, null)
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (selectedExpenseForView != null) {
        ExpenseDetailDialog(
            expense = selectedExpenseForView!!,
            onDismiss = { selectedExpenseForView = null },
            onEdit = {
                val expense = selectedExpenseForView!!
                selectedExpenseForView = null
                onNavigateToExpenseEntry(
                    try { LocalDate.parse(expense.date) } catch(e: Exception) { LocalDate.now() },
                    expense.id
                )
            },
            onImageClick = { selectedImagePathForPreview = it }
        )
    }

    if (selectedImagePathForPreview != null) {
        ImagePreviewDialog(
            imagePath = selectedImagePathForPreview!!,
            onDismiss = { selectedImagePathForPreview = null }
        )
    }

    HomeScreenContent(
        recentExpenses = recentExpenses,
        dailySummary = dailySummary,
        onNavigateToExpenseEntry = onNavigateToExpenseEntry,
        onNavigateToReports = onNavigateToReports,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToPayments = onNavigateToPayments,
        onNavigateToWeeklyFunds = onNavigateToWeeklyFunds,
        onNavigateToSearch = onNavigateToSearch,
        onShowDatePicker = { showDatePicker = true },
        onViewExpense = { selectedExpenseForView = it }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    recentExpenses: List<DailyExpense>,
    dailySummary: ExpenseSummary,
    onNavigateToExpenseEntry: (LocalDate, Int?) -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPayments: () -> Unit,
    onNavigateToWeeklyFunds: () -> Unit,
    onNavigateToSearch: () -> Unit = {},
    onShowDatePicker: () -> Unit = {},
    onViewExpense: (DailyExpense) -> Unit = {}
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Estate Expense Tracker") },
                actions = {
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToExpenseEntry(LocalDate.now(), null) }
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Expense")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // Quick Stats Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Insights, contentDescription = null, tint = Color.White,)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Estate Investment Summary",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatItem(
                                label = "Income",
                                value = "₹${dailySummary.totalIncome}",
                                color = Color.White,
                                icon = Icons.AutoMirrored.Filled.TrendingUp
                            )
                            StatItem(
                                label = "Expenses",
                                value = "₹${dailySummary.totalLaborCost + dailySummary.totalOvertimeCost + dailySummary.totalOtherExpenses}",
                                color = MaterialTheme.colorScheme.error,
                                icon = Icons.AutoMirrored.Filled.TrendingDown
                            )

                            StatItem(
                                label = if (dailySummary.totalExcessBalance >= BigDecimal.ZERO) "Excess Balance" else "Shortfall",
                                value = "₹${dailySummary.totalExcessBalance}",
                                color = Color.White,
                                icon = Icons.AutoMirrored.Filled.ArrowForward
                            )
                        }
                    }
                }
            }

            // Quick Actions
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Quick Actions",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onNavigateToExpenseEntry(LocalDate.now(), null) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Today, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Today")
                            }

                            OutlinedButton(
                                onClick = onShowDatePicker,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add for Date")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onNavigateToReports,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Reports",
                                    fontSize = MaterialTheme.typography.labelSmall.fontSize,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            Button(
                                onClick = onNavigateToPayments,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Payments",
                                    fontSize = MaterialTheme.typography.labelSmall.fontSize,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            Button(
                                onClick = onNavigateToWeeklyFunds,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.ListAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Weekly",
                                    fontSize = MaterialTheme.typography.labelSmall.fontSize,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            // Recent Expenses
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Expenses",
                        style = MaterialTheme.typography.titleMedium
                    )
                    TextButton(onClick = onNavigateToReports) {
                        Text("View All")
                    }
                }
            }

            items(recentExpenses.take(12)) { expense ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onViewExpense(expense) }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = try {
                                    LocalDate.parse(expense.date).format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                                } catch (e: Exception) {
                                    expense.date
                                },
                                style = MaterialTheme.typography.titleSmall
                            )
                            Row {
                                Text(
                                    text = "Exp: ₹${expense.totalLaborCost + expense.totalOvertimeCost + expense.totalOtherExpensesCost}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Pay: ₹${expense.advanceAmount + expense.excessBalance + expense.weeklyPaymentDone}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Inc: ₹${expense.totalIncome}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Worker summary
                        val totalWorkers = expense.malayaliMaleCount + expense.bengaliMaleCount +
                                          expense.malayaliFemaleCount + expense.bengaliFemaleCount
                        if (totalWorkers > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Workers: $totalWorkers (${expense.malayaliMaleCount}M + ${expense.bengaliMaleCount}M + ${expense.malayaliFemaleCount}F + ${expense.bengaliFemaleCount}F)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (expense.comments.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = expense.comments,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Show message if no data
            if (recentExpenses.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No expenses recorded yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { onNavigateToExpenseEntry(LocalDate.now(), null) }) {
                                Text("Add Your First Expense")
                            }
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = color
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = color
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailDialog(
    expense: DailyExpense,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onImageClick: (String) -> Unit = {}
) {
    val workerGroups = remember(expense.workerGroups) {
        try {
            if (expense.workerGroups.isBlank() || expense.workerGroups == "[]") emptyList<WorkerGroupEntry>()
            else kotlinx.serialization.json.Json.decodeFromString(ListSerializer(WorkerGroupEntry.serializer()), expense.workerGroups)
        } catch (e: Exception) { emptyList() }
    }
    
    val otherExpenses = remember(expense.otherExpenses) {
        try {
            if (expense.otherExpenses.isBlank() || expense.otherExpenses == "[]") emptyList<OtherExpenseEntry>()
            else kotlinx.serialization.json.Json.decodeFromString(ListSerializer(OtherExpenseEntry.serializer()), expense.otherExpenses)
        } catch (e: Exception) { emptyList() }
    }

    val incomeEntries = remember(expense.incomeEntries) {
        try {
            if (expense.incomeEntries.isBlank() || expense.incomeEntries == "[]") emptyList<IncomeEntry>()
            else kotlinx.serialization.json.Json.decodeFromString(ListSerializer(IncomeEntry.serializer()), expense.incomeEntries)
        } catch (e: Exception) { emptyList() }
    }

    val advanceEntries = remember(expense.advanceEntries) {
        try {
            if (expense.advanceEntries.isBlank() || expense.advanceEntries == "[]") emptyList<AdvanceEntry>()
            else kotlinx.serialization.json.Json.decodeFromString(ListSerializer(AdvanceEntry.serializer()), expense.advanceEntries)
        } catch (e: Exception) { emptyList() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = try {
                        LocalDate.parse(expense.date).format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                    } catch (e: Exception) { expense.date },
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Summary Stats
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        DetailRow("Total Labor", "₹${expense.totalLaborCost}")
                        DetailRow("Total Overtime", "₹${expense.totalOvertimeCost}")
                        DetailRow("Other Expenses", "₹${expense.totalOtherExpensesCost}")
                        DetailRow("Advances Paid", "₹${expense.advanceAmount}")
                        DetailRow("Weekly Settlement", "₹${expense.weeklyPaymentDone}")
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        DetailRow("TOTAL INCOME", "₹${expense.totalIncome}", isTotal = true, color = MaterialTheme.colorScheme.primary)
                    }
                }

                // Labor Breakdown
                if (workerGroups.isNotEmpty()) {
                    SectionTitle("Labor Breakdown")
                    workerGroups.forEach { group ->
                        Column(modifier = Modifier.fillMaxWidth().padding(start = 8.dp)) {
                            Text("${group.workerTypeName} (${group.count})", style = MaterialTheme.typography.bodyMedium)
                            Text("Task: ${group.taskPerformed}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                            Text("Cost: ₹${group.calculateTotalGroupCost()}", style = MaterialTheme.typography.bodySmall)
                            if (group.comments.isNotEmpty()) {
                                Text(group.comments, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        }
                    }
                }

                // Other Expenses Breakdown
                if (otherExpenses.isNotEmpty()) {
                    SectionTitle("Other Expenses")
                    otherExpenses.forEach { entry ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.typeName, style = MaterialTheme.typography.bodyMedium)
                                if (entry.notes.isNotEmpty()) {
                                    Text(entry.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                }
                                Text("Qty: ${entry.quantity} | ₹${entry.amount}", style = MaterialTheme.typography.bodySmall)
                            }
                            
                            if (entry.receiptImagePath != null) {
                                AsyncImage(
                                    model = File(entry.receiptImagePath),
                                    contentDescription = "Receipt",
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { onImageClick(entry.receiptImagePath) },
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    }
                }

                // Income Breakdown
                if (incomeEntries.isNotEmpty()) {
                    SectionTitle("Income Details")
                    incomeEntries.forEach { entry ->
                        Column(modifier = Modifier.fillMaxWidth().padding(start = 8.dp)) {
                            Text(entry.typeName, style = MaterialTheme.typography.bodyMedium)
                            Text("${entry.weight} kg @ ₹${entry.pricePerKilo}/kg", style = MaterialTheme.typography.bodySmall)
                            Text("Total: ₹${entry.amount}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        }
                    }
                }

                // Advances
                if (advanceEntries.isNotEmpty()) {
                    SectionTitle("Advance Payments")
                    advanceEntries.forEach { entry ->
                        Column(modifier = Modifier.fillMaxWidth().padding(start = 8.dp)) {
                            Text("Recipient: ${entry.recipientName}", style = MaterialTheme.typography.bodyMedium)
                            Text("Amount: ₹${entry.amount}", style = MaterialTheme.typography.bodySmall)
                            if (entry.reason.isNotEmpty()) {
                                Text("Reason: ${entry.reason}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        }
                    }
                }

                if (expense.comments.isNotEmpty()) {
                    SectionTitle("Daily Comments")
                    Text(
                        text = expense.comments,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit Details")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun DetailRow(label: String, value: String, isTotal: Boolean = false, color: Color = Color.Unspecified) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = if (isTotal) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodySmall
        )
        Text(
            text = value,
            style = if (isTotal) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodySmall,
            color = if (color != Color.Unspecified) color else if (isTotal) MaterialTheme.colorScheme.primary else Color.Unspecified
        )
    }
}

@Composable
fun ImagePreviewDialog(
    imagePath: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = File(imagePath),
                contentDescription = "Full Size Receipt",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }
    }
}
