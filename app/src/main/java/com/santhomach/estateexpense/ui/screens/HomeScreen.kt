package com.santhomach.estateexpense.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
import com.santhomach.estateexpense.data.model.DailyExpense
import com.santhomach.estateexpense.data.repository.ExpenseSummary
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToExpenseEntry: (LocalDate, Int?) -> Unit = { _, _ -> },
    onNavigateToReports: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToPayments: () -> Unit = {},
    onNavigateToWeeklyFunds: () -> Unit = {},
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
            onNavigateToWeeklyFunds = onNavigateToWeeklyFunds
        )
        return
    }

    val actualViewModel: ReportsViewModel = viewModel ?: hiltViewModel()
    val recentExpenses by actualViewModel.recentExpenses.collectAsState()
    val dailySummary by actualViewModel.dailySummary.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }

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

    HomeScreenContent(
        recentExpenses = recentExpenses,
        dailySummary = dailySummary,
        onNavigateToExpenseEntry = onNavigateToExpenseEntry,
        onNavigateToReports = onNavigateToReports,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToPayments = onNavigateToPayments,
        onNavigateToWeeklyFunds = onNavigateToWeeklyFunds,
        onShowDatePicker = { showDatePicker = true }
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
    onShowDatePicker: () -> Unit = {}
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Estate Expense Tracker") },
                actions = {
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
                                value = "₹${dailySummary.totalLaborCost + dailySummary.totalOvertimeCost + dailySummary.totalOtherExpenses + dailySummary.totalAdvanceAmount + dailySummary.totalExcessBalance + dailySummary.totalWeeklyPayment}",
                                color = MaterialTheme.colorScheme.error,
                                icon = Icons.AutoMirrored.Filled.TrendingDown
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
                    onClick = { 
                        onNavigateToExpenseEntry(
                            try { LocalDate.parse(expense.date) } catch(e: Exception) { LocalDate.now() },
                            expense.id
                        )
                    }
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
                                    text = "Exp: ₹${expense.totalLaborCost + expense.totalOvertimeCost + expense.totalOtherExpensesCost + expense.advanceAmount + expense.excessBalance + expense.weeklyPaymentDone}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.error
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
