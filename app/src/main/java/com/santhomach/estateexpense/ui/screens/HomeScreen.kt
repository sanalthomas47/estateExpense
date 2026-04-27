package com.santhomach.estateexpense.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Save
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    HomeScreenContent(
        recentExpenses = recentExpenses,
        dailySummary = dailySummary,
        onNavigateToExpenseEntry = onNavigateToExpenseEntry,
        onNavigateToReports = onNavigateToReports,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToPayments = onNavigateToPayments,
        onNavigateToWeeklyFunds = onNavigateToWeeklyFunds
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
    onNavigateToWeeklyFunds: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Estate Expense Tracker") },
                actions = {
                    IconButton(onClick = { /* Nothing to save here */ }) {
                        Icon(Icons.Filled.Save, contentDescription = "Save")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
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
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Today's Summary",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatItem(
                                label = "Income",
                                value = "₹${dailySummary.totalIncome}",
                                color = MaterialTheme.colorScheme.primary
                            )
                            StatItem(
                                label = "Expenses",
                                value = "₹${dailySummary.totalLaborCost + dailySummary.totalOvertimeCost + dailySummary.totalOtherExpenses}",
                                color = MaterialTheme.colorScheme.error
                            )
                            StatItem(
                                label = "Net",
                                value = "₹${dailySummary.netAmount}",
                                color = if (dailySummary.netAmount >= BigDecimal.ZERO)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.error
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
                                Text("Add Today")
                            }

                            OutlinedButton(
                                onClick = { onNavigateToExpenseEntry(LocalDate.now().plusDays(1), null) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Add Tomorrow")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onNavigateToReports,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Reports")
                            }

                            Button(
                                onClick = onNavigateToPayments,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Payments")
                            }

                            Button(
                                onClick = onNavigateToWeeklyFunds,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Weekly")
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

            items(recentExpenses.take(5)) { expense ->
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
                            Text(
                                text = "₹${expense.calculateNetAmount()}",
                                style = MaterialTheme.typography.titleSmall,
                                color = if (expense.calculateNetAmount() >= BigDecimal.ZERO)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.error
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Income: ₹${expense.totalIncome}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Expenses: ₹${expense.totalLaborCost + expense.totalOvertimeCost + expense.totalOtherExpensesCost}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

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
    color: androidx.compose.ui.graphics.Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = color
        )
    }
}
