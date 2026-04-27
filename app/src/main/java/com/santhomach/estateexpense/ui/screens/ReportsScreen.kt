package com.santhomach.estateexpense.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.santhomach.estateexpense.ui.viewmodel.DateRange
import com.santhomach.estateexpense.ui.viewmodel.ReportsViewModel
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import androidx.compose.ui.platform.LocalInspectionMode
import com.santhomach.estateexpense.ui.viewmodel.ReportsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToExpenseEntry: (LocalDate, Int?) -> Unit = { _, _ -> },
    viewModelArg: ReportsViewModel? = null
) {
    if (LocalInspectionMode.current && viewModelArg == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Reports Screen Preview")
        }
        return
    }

    val viewModel: ReportsViewModel = viewModelArg ?: hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val dateRange by viewModel.dateRange.collectAsState()
    val dailySummary by viewModel.dailySummary.collectAsState()
    val weeklySummary by viewModel.weeklySummary.collectAsState()
    val filteredExpenses by viewModel.filteredExpenses.collectAsState()

    var showDateRangePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports & Analytics") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Could implement export to CSV/PDF */ }) {
                        Icon(Icons.Filled.Save, contentDescription = "Save Report")
                    }
                    IconButton(onClick = { viewModel.refreshData() }) {
                        if (uiState.isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        } else {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                        }
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
            // Date Range Selector
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Date Range",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        var expanded by remember { mutableStateOf(false) }
                        val rangeOptions = listOf(
                            "Last 7 Days" to DateRange.Last7Days,
                            "Last 30 Days" to DateRange.Last30Days,
                            "Last 90 Days" to DateRange.Last90Days,
                            "This Month" to DateRange.ThisMonth,
                            "Last Month" to DateRange.LastMonth,
                            "This Year" to DateRange.ThisYear,
                            "Custom Range" to DateRange.Custom(LocalDate.now().minusDays(30), LocalDate.now())
                        )

                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it }
                        ) {
                            OutlinedTextField(
                                value = when (dateRange) {
                                    is DateRange.Last7Days -> "Last 7 Days"
                                    is DateRange.Last30Days -> "Last 30 Days"
                                    is DateRange.Last90Days -> "Last 90 Days"
                                    is DateRange.LastYear -> "Last Year"
                                    is DateRange.ThisMonth -> "This Month"
                                    is DateRange.LastMonth -> "Last Month"
                                    is DateRange.ThisYear -> "This Year"
                                    is DateRange.Custom -> "Custom Range"
                                },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Select Range") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                rangeOptions.forEach { (label, range) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            viewModel.setDateRange(range)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Daily Summary
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Daily Summary",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        SummaryRow("Total Income", "₹${dailySummary.totalIncome}")
                        SummaryRow("Total Labor Cost", "₹${dailySummary.totalLaborCost}")
                        SummaryRow("Total Overtime Cost", "₹${dailySummary.totalOvertimeCost}")
                        SummaryRow("Total Advance Paid", "₹${dailySummary.totalAdvanceAmount}")
                        SummaryRow("Total Other Expenses", "₹${dailySummary.totalOtherExpenses}")
                        SummaryRow("Previous Excess Balance", "₹${dailySummary.totalExcessBalance}")
                        
                        val totalExp = dailySummary.totalLaborCost + dailySummary.totalOvertimeCost + dailySummary.totalOtherExpenses + dailySummary.totalAdvanceAmount + dailySummary.totalExcessBalance
                        
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        SummaryRow("TOTAL EXPENSES", "₹$totalExp", isTotal = true)
                        SummaryRow("TOTAL INCOME", "₹${dailySummary.totalIncome}", isTotal = true)
                        
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                        SummaryRow("Total Days", dailySummary.totalDays.toString())
                        SummaryRow("Avg Daily Income", "₹${dailySummary.averageDailyIncome}")
                        SummaryRow("Avg Daily Expense", "₹${dailySummary.averageDailyExpense}")
                    }
                }
            }

            // Weekly Summary
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Weekly Summary",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        SummaryRow("Total Income", "₹${weeklySummary.totalIncome}")
                        SummaryRow("Total Labor Cost", "₹${weeklySummary.totalLaborCost}")
                        SummaryRow("Total Overtime Cost", "₹${weeklySummary.totalOvertimeCost}")
                        SummaryRow("Total Advance Paid", "₹${weeklySummary.totalAdvanceAmount}")
                        SummaryRow("Total Other Expenses", "₹${weeklySummary.totalOtherExpenses}")
                        SummaryRow("Previous Excess Balance", "₹${weeklySummary.totalExcessBalance}")
                        
                        val totalExp = weeklySummary.totalLaborCost + weeklySummary.totalOvertimeCost + weeklySummary.totalOtherExpenses + weeklySummary.totalAdvanceAmount + weeklySummary.totalExcessBalance
                        
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        SummaryRow("TOTAL EXPENSES", "₹$totalExp", isTotal = true)
                        SummaryRow("TOTAL INCOME", "₹${weeklySummary.totalIncome}", isTotal = true)
                        
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                        SummaryRow("Total Weeks", weeklySummary.totalWeeks.toString())
                        SummaryRow("Avg Weekly Income", "₹${weeklySummary.averageWeeklyIncome}")
                        SummaryRow("Avg Weekly Expense", "₹${weeklySummary.averageWeeklyExpense}")
                    }
                }
            }

            // Top Expense Categories
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Top Expense Categories",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        val topCategories = viewModel.getTopExpenseCategories(filteredExpenses)
                        if (topCategories.isEmpty()) {
                            Text(
                                text = "No expense data available",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            topCategories.forEach { category ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = category.categoryName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "₹${category.totalAmount}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                if (category != topCategories.last()) {
                                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Income Breakdown
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Income Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        val incomeBreakdown = viewModel.getIncomeBreakdown(filteredExpenses)
                        if (incomeBreakdown.isEmpty()) {
                            Text(
                                text = "No income data available",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            incomeBreakdown.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.commodityName,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "Total Weight: ${item.totalWeight} kg",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    Text(
                                        text = "₹${item.totalAmount}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (item != incomeBreakdown.last()) {
                                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Recent Expenses List
            item {
                Text(
                    text = "Recent Expenses",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(filteredExpenses.take(20)) { expense ->
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
                                    text = "Exp: ₹${expense.totalLaborCost + expense.totalOvertimeCost + expense.totalOtherExpensesCost + expense.advanceAmount + expense.excessBalance}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Inc: ₹${expense.totalIncome}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

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
            if (filteredExpenses.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No expense data found for the selected date range",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Error Snackbar
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Show snackbar
            viewModel.clearError()
        }
    }
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
            style = if (isTotal) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            color = if (isTotal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
