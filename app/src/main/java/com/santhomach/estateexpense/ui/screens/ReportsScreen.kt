package com.santhomach.estateexpense.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import java.time.Instant
import java.time.ZoneId

import androidx.compose.ui.platform.LocalInspectionMode
import com.santhomach.estateexpense.ui.viewmodel.ReportsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    startDate: LocalDate? = null,
    endDate: LocalDate? = null,
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
    
    // Set initial date range if provided via navigation
    LaunchedEffect(startDate, endDate) {
        if (startDate != null && endDate != null) {
            viewModel.setDateRange(DateRange.Custom(startDate, endDate))
        }
    }
    val uiState by viewModel.uiState.collectAsState()
    val dateRange by viewModel.dateRange.collectAsState()
    val dailySummary by viewModel.dailySummary.collectAsState()
    val weeklySummary by viewModel.weeklySummary.collectAsState()
    val filteredExpenses by viewModel.filteredExpenses.collectAsState()

    var showDateRangePicker by remember { mutableStateOf(false) }

    if (showDateRangePicker) {
        val dateRangePickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showDateRangePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val start = dateRangePickerState.selectedStartDateMillis?.let {
                            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                        }
                        val end = dateRangePickerState.selectedEndDateMillis?.let {
                            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                        }
                        if (start != null && end != null) {
                            viewModel.setDateRange(DateRange.Custom(start, end))
                            showDateRangePicker = false
                        }
                    },
                    enabled = dateRangePickerState.selectedStartDateMillis != null && dateRangePickerState.selectedEndDateMillis != null
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDateRangePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                title = { Text("Select Date Range", modifier = Modifier.padding(16.dp)) },
                modifier = Modifier.fillMaxWidth().height(500.dp)
            )
        }
    }

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
                            "Current Week" to DateRange.CurrentWeek,
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
                                value = when (val range = dateRange) {
                                    is DateRange.CurrentWeek -> "Current Week"
                                    is DateRange.Last7Days -> "Last 7 Days"
                                    is DateRange.Last30Days -> "Last 30 Days"
                                    is DateRange.Last90Days -> "Last 90 Days"
                                    is DateRange.LastYear -> "Last Year"
                                    is DateRange.ThisMonth -> "This Month"
                                    is DateRange.LastMonth -> "Last Month"
                                    is DateRange.ThisYear -> "This Year"
                                    is DateRange.Custom -> "${range.startDate.format(DateTimeFormatter.ofPattern("dd MMM"))} - ${range.endDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))}"
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
                                            if (range is DateRange.Custom) {
                                                showDateRangePicker = true
                                            } else {
                                                viewModel.setDateRange(range)
                                            }
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

                        SummaryRow("Total Income", "₹${dailySummary.totalIncome}", icon = Icons.Default.TrendingUp, iconColor = MaterialTheme.colorScheme.primary)
                        SummaryRow("Total Labor Cost", "₹${dailySummary.totalLaborCost}", icon = Icons.Default.Groups)
                        SummaryRow("Total Overtime Cost", "₹${dailySummary.totalOvertimeCost}", icon = Icons.Default.AccessTime)
                        SummaryRow("Total Advance Paid", "₹${dailySummary.totalAdvanceAmount}", icon = Icons.Default.Payments)
                        SummaryRow("Weekly Payment Done", "₹${dailySummary.totalWeeklyPayment}", icon = Icons.Default.DoneAll)
                        SummaryRow("Total Other Expenses", "₹${dailySummary.totalOtherExpenses}", icon = Icons.Default.ShoppingBag)
                        SummaryRow("Previous Excess Balance", "₹${dailySummary.totalExcessBalance}", icon = Icons.Default.AccountBalanceWallet)
                        
                        val totalExp = dailySummary.totalLaborCost + dailySummary.totalOvertimeCost + dailySummary.totalOtherExpenses
                        val totalOffset = (weeklySummary.totalLaborCost + weeklySummary.totalOvertimeCost + weeklySummary.totalOtherExpenses) - ( weeklySummary.totalAdvanceAmount + weeklySummary.totalWeeklyPayment + weeklySummary.totalExcessBalance)
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        SummaryRow("TOTAL EXPENSES", "₹$totalExp", isTotal = true)
                        SummaryRow("TOTAL OFFSET (EXPENSES - PAYMENTS)", "₹$totalOffset", isTotal = true)
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

                        SummaryRow("Total Income", "₹${weeklySummary.totalIncome}", icon = Icons.Default.TrendingUp, iconColor = MaterialTheme.colorScheme.primary)
                        SummaryRow("Total Labor Cost", "₹${weeklySummary.totalLaborCost}", icon = Icons.Default.Groups)
                        SummaryRow("Total Overtime Cost", "₹${weeklySummary.totalOvertimeCost}", icon = Icons.Default.AccessTime)
                        SummaryRow("Total Advance Paid", "₹${weeklySummary.totalAdvanceAmount}", icon = Icons.Default.Payments)
                        SummaryRow("Weekly Payment Done", "₹${weeklySummary.totalWeeklyPayment}", icon = Icons.Default.DoneAll)
                        SummaryRow("Total Other Expenses", "₹${weeklySummary.totalOtherExpenses}", icon = Icons.Default.ShoppingBag)
                        SummaryRow("Previous Excess Balance", "₹${weeklySummary.totalExcessBalance}", icon = Icons.Default.AccountBalanceWallet)
                        
                        val totalExp = weeklySummary.totalLaborCost + weeklySummary.totalOvertimeCost + weeklySummary.totalOtherExpenses
                        val totalOffset = (weeklySummary.totalLaborCost + weeklySummary.totalOvertimeCost + weeklySummary.totalOtherExpenses) - ( weeklySummary.totalAdvanceAmount + weeklySummary.totalWeeklyPayment + weeklySummary.totalExcessBalance)
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        SummaryRow("TOTAL EXPENSES", "₹$totalExp", isTotal = true)
                        SummaryRow("TOTAL OFFSET (EXPENSES - PAYMENTS)", "₹$totalOffset", isTotal = true)
                        SummaryRow("TOTAL INCOME", "₹${weeklySummary.totalIncome}", isTotal = true)
                        
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                        SummaryRow("Total Weeks", weeklySummary.totalWeeks.toString())
                        SummaryRow("Avg Weekly Income", "₹${weeklySummary.averageWeeklyIncome}")
                        SummaryRow("Avg Weekly Expense", "₹${weeklySummary.averageWeeklyExpense}")
                    }
                }
            }

            // Weekly Specifics
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Weekly Specifics",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        val specificWorkers = viewModel.getWeeklySpecificWorkers(filteredExpenses)
                        val specificOther = viewModel.getWeeklySpecificOtherExpenses(filteredExpenses)
                        val specificSettlement = viewModel.getWeeklySpecificSettlement(filteredExpenses)
                        val specificAdvances = viewModel.getWeeklySpecificAdvances(filteredExpenses)
                        val hasOvertime = specificWorkers.any { it.totalOvertimeCost > BigDecimal.ZERO }

                        if (specificWorkers.isEmpty() && specificOther.isEmpty() && specificSettlement == BigDecimal.ZERO && specificAdvances.isEmpty()) {
                            Text("No data for this period", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            // LABOR
                            if (specificWorkers.isNotEmpty()) {
                                Text(
                                    "LABOR",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Type", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(2f))
                                    Text("Days", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(0.6f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                                    Text("Cost", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                                }
                                Divider()
                                specificWorkers.forEach { row ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(row.workerTypeName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(2f))
                                        Text(row.totalCount.toString(), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.6f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                                        Text("₹${row.totalBaseCost}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                                    }
                                    Divider()
                                }
                            }

                            // OTHER EXPENSES
                            if (specificOther.isNotEmpty()) {
                                Text(
                                    "OTHER EXPENSES",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 2.dp)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Item", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(2f))
                                    Text("Qty", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(0.6f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                                    Text("Amount", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                                }
                                Divider()
                                specificOther.forEach { row ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val label = if (row.subtypeName != null) "${row.typeName} (${row.subtypeName})" else row.typeName
                                        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(2f))
                                        Text(
                                            if (row.totalQuantity == row.totalQuantity.toLong().toDouble()) row.totalQuantity.toLong().toString() else String.format("%.1f", row.totalQuantity),
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(0.6f),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                                        )
                                        Text("₹${row.totalAmount}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                                    }
                                    Divider()
                                }
                            }

                            // OVERTIME
                            if (hasOvertime) {
                                Text(
                                    "OVERTIME",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 2.dp)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Type", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(2f))
                                    Text("OT Cost", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                                }
                                Divider()
                                specificWorkers.filter { it.totalOvertimeCost > BigDecimal.ZERO }.forEach { row ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(row.workerTypeName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(2f))
                                        Text("₹${row.totalOvertimeCost}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                                    }
                                    Divider()
                                }
                            }

                            // ADVANCES
                            if (specificAdvances.isNotEmpty()) {
                                Text(
                                    "ADVANCES",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 2.dp)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Recipient", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(2f))
                                    Text("Amount", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                                }
                                Divider()
                                specificAdvances.forEach { row ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(2f)) {
                                            Text(row.recipientName, style = MaterialTheme.typography.bodyMedium)
                                            if (row.reason.isNotEmpty()) {
                                                Text(row.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                            }
                                        }
                                        Text("₹${row.totalAmount}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                                    }
                                    Divider()
                                }
                            }

                            // SETTLEMENT
                            if (specificSettlement > BigDecimal.ZERO) {
                                Text(
                                    "SETTLEMENT",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 2.dp)
                                )
                                Divider()
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Weekly Settlement Done", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(2f))
                                    Text("₹$specificSettlement", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                                }
                                Divider()
                            }
                        }
                    }
                }
            }

            // Weekly Worker Summary Table
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Labor Summary (Grouped)",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        val workerSummary = viewModel.getWeeklyWorkerSummary(filteredExpenses)
                        if (workerSummary.isEmpty()) {
                            Text("No labor data for this period", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            // Table Header
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Type & Comment", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(2f))
                                Text("Count", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(0.5f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                                Text("Total Cost", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                            }
                            Divider()
                            workerSummary.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(2f)) {
                                        Text(item.workerType, style = MaterialTheme.typography.bodyMedium)
                                        if (item.comment.isNotEmpty()) {
                                            Text(item.comment, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                        }
                                    }
                                    Text(item.totalCount.toString(), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.5f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                                    Text("₹${item.totalCost}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                                }
                                if (item != workerSummary.last()) {
                                    Divider()
                                }
                            }
                        }
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

            items(filteredExpenses.take(20), key = { it.id }) { expense ->
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
    isTotal: Boolean = false,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.secondary
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = iconColor
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = label,
                style = if (isTotal) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium
            )
        }
        Text(
            text = value,
            style = if (isTotal) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            color = if (isTotal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
