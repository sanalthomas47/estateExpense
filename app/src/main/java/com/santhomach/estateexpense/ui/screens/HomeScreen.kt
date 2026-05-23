package com.santhomach.estateexpense.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.santhomach.estateexpense.data.model.*
import com.santhomach.estateexpense.data.repository.ExpenseSummary
import com.santhomach.estateexpense.ui.viewmodel.ReportsViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import java.io.File
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
    onNavigateToSearch: () -> Unit = {},
    onNavigateToExpenseTypeSummary: () -> Unit = {},
    viewModel: ReportsViewModel? = null
) {
    if (LocalInspectionMode.current && viewModel == null) {
        HomeScreenContent(
            recentExpenses = emptyList(),
            dailySummary = ExpenseSummary(),
            weekSummary = ExpenseSummary(),
            yearSummary = ExpenseSummary(),
            allTimeSummary = ExpenseSummary(),
            onNavigateToExpenseEntry = onNavigateToExpenseEntry,
            onNavigateToReports = onNavigateToReports,
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToPayments = onNavigateToPayments,
            onNavigateToWeeklyFunds = onNavigateToWeeklyFunds,
            onNavigateToSearch = onNavigateToSearch,
            onNavigateToExpenseTypeSummary = onNavigateToExpenseTypeSummary
        )
        return
    }

    val actualViewModel: ReportsViewModel = viewModel ?: hiltViewModel()
    val recentExpenses by actualViewModel.yearlyExpenses.collectAsState()
    val dailySummary by actualViewModel.dailySummary.collectAsState()
    val weekSummary by actualViewModel.weekSummary.collectAsState()
    val yearSummary by actualViewModel.yearSummary.collectAsState()
    val allTimeSummary by actualViewModel.allTimeSummary.collectAsState()
    val uiState by actualViewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showDatePicker by remember { mutableStateOf(false) }
    var selectedExpenseForView by remember { mutableStateOf<DailyExpense?>(null) }
    var selectedImagePathForPreview by remember { mutableStateOf<String?>(null) }
    var expenseToClone by remember { mutableStateOf<DailyExpense?>(null) }

    LaunchedEffect(uiState.cloneSuccessDate) {
        val date = uiState.cloneSuccessDate ?: return@LaunchedEffect
        val label = date.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
        snackbarHostState.showSnackbar("Cloned to $label")
        actualViewModel.clearCloneSuccess()
    }

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
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
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
                    try { LocalDate.parse(expense.date) } catch (e: Exception) { LocalDate.now() },
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

    if (expenseToClone != null) {
        CloneExpenseDialog(
            expense = expenseToClone!!,
            nextWorkday = actualViewModel.nextWorkday(
                try { LocalDate.parse(expenseToClone!!.date) } catch (_: Exception) { LocalDate.now() }
            ),
            onDismiss = { expenseToClone = null },
            onClone = { targetDate ->
                actualViewModel.cloneExpense(expenseToClone!!, targetDate)
                expenseToClone = null
            }
        )
    }

    HomeScreenContent(
        recentExpenses = recentExpenses,
        dailySummary = dailySummary,
        weekSummary = weekSummary,
        yearSummary = yearSummary,
        allTimeSummary = allTimeSummary,
        snackbarHostState = snackbarHostState,
        onNavigateToExpenseEntry = onNavigateToExpenseEntry,
        onNavigateToReports = onNavigateToReports,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToPayments = onNavigateToPayments,
        onNavigateToWeeklyFunds = onNavigateToWeeklyFunds,
        onNavigateToSearch = onNavigateToSearch,
        onNavigateToExpenseTypeSummary = onNavigateToExpenseTypeSummary,
        onShowDatePicker = { showDatePicker = true },
        onViewExpense = { selectedExpenseForView = it },
        onLongPressExpense = { expenseToClone = it }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreenContent(
    recentExpenses: List<DailyExpense>,
    dailySummary: ExpenseSummary,
    weekSummary: ExpenseSummary,
    yearSummary: ExpenseSummary,
    allTimeSummary: ExpenseSummary,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onNavigateToExpenseEntry: (LocalDate, Int?) -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPayments: () -> Unit,
    onNavigateToWeeklyFunds: () -> Unit,
    onNavigateToSearch: () -> Unit = {},
    onNavigateToExpenseTypeSummary: () -> Unit = {},
    onShowDatePicker: () -> Unit = {},
    onViewExpense: (DailyExpense) -> Unit = {},
    onLongPressExpense: (DailyExpense) -> Unit = {}
) {
    val grouped = remember(recentExpenses) {
        recentExpenses
            .groupBy { expense ->
                try {
                    val d = LocalDate.parse(expense.date)
                    "${d.year}-${d.monthValue.toString().padStart(2, '0')}"
                } catch (_: Exception) { "Unknown" }
            }
            .entries
            .sortedByDescending { it.key }
            .map { (key, list) ->
                val label = try {
                    val parts = key.split("-")
                    LocalDate.of(parts[0].toInt(), parts[1].toInt(), 1)
                        .format(DateTimeFormatter.ofPattern("MMMM yyyy"))
                } catch (_: Exception) { key }
                Triple(key, label, list)
            }
    }
    val expandedMonths = remember { mutableStateMapOf<String, Boolean>() }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "ESTATE LEDGER",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = LocalDate.now()
                                .format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy")),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToExpenseEntry(LocalDate.now(), null) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Expense")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    PerformanceHeroCard(
                        weekSummary = weekSummary,
                        yearSummary = yearSummary,
                        allTimeSummary = allTimeSummary
                    )
                }

                item {
                    QuickActionsSection(
                        onAddToday = { onNavigateToExpenseEntry(LocalDate.now(), null) },
                        onAddForDate = onShowDatePicker,
                        onReports = onNavigateToReports,
                        onPayments = onNavigateToPayments,
                        onWeekly = onNavigateToWeeklyFunds,
                        onExpenseTypes = onNavigateToExpenseTypeSummary
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Activity",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(onClick = onNavigateToReports) {
                            Text("View All")
                        }
                    }
                }

                grouped.forEach { (monthKey, monthLabel, expenses) ->
                    val isExpanded = expandedMonths.getOrElse(monthKey) { true }

                    item(key = "header_$monthKey") {
                        MonthGroupHeader(
                            label = monthLabel,
                            count = expenses.size,
                            isExpanded = isExpanded,
                            onToggle = { expandedMonths[monthKey] = !isExpanded }
                        )
                    }

                    if (isExpanded) {
                        items(expenses, key = { it.id }) { expense ->
                            ExpenseListCard(
                                expense = expense,
                                onClick = { onViewExpense(expense) },
                                onLongClick = { onLongPressExpense(expense) }
                            )
                        }
                    }
                }

                if (recentExpenses.isEmpty()) {
                    item {
                        EmptyStateCard(
                            onClick = { onNavigateToExpenseEntry(LocalDate.now(), null) }
                        )
                    }
                }
            }
        }
    }
}

// ─── Hero performance card ────────────────────────────────────────────────────

@Composable
private fun PerformanceHeroCard(
    weekSummary: ExpenseSummary,
    yearSummary: ExpenseSummary,
    allTimeSummary: ExpenseSummary
) {
    val weekExpense = weekSummary.totalLaborCost + weekSummary.totalOvertimeCost + weekSummary.totalOtherExpenses
    val weekBalance = (weekSummary.totalAdvanceAmount + weekSummary.totalWeeklyPayment) - weekExpense
    val yearExpense = yearSummary.totalLaborCost + yearSummary.totalOvertimeCost + yearSummary.totalOtherExpenses
    val allExpense = allTimeSummary.totalLaborCost + allTimeSummary.totalOvertimeCost + allTimeSummary.totalOtherExpenses

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF1A5C3A), Color(0xFF062015))
                )
            )
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Insights,
                    contentDescription = null,
                    tint = Color(0xFF52D68A),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Estate Performance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            SummaryLine(
                title = "THIS WEEK",
                income = weekSummary.totalIncome,
                expense = weekExpense,
                balance = weekBalance,
                showEfficiency = false
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.15f), thickness = 0.5.dp)

            SummaryLine(
                title = "THIS YEAR",
                income = yearSummary.totalIncome,
                expense = yearExpense,
                showEfficiency = true
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.15f), thickness = 0.5.dp)

            SummaryLine(
                title = "ALL TIME",
                income = allTimeSummary.totalIncome,
                expense = allExpense,
                showEfficiency = true
            )
        }
    }
}

@Composable
private fun SummaryLine(
    title: String,
    income: BigDecimal,
    expense: BigDecimal,
    balance: BigDecimal? = null,
    showEfficiency: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.55f),
            letterSpacing = 1.2.sp
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            StatItemMini(label = "Income",  value = "₹$income",  color = Color(0xFF80FFB2), modifier = Modifier.weight(1f))
            StatItemMini(label = "Expense", value = "₹$expense", color = Color(0xFFFFADAD), modifier = Modifier.weight(1f))
            when {
                balance != null -> StatItemMini(
                    label = if (balance >= BigDecimal.ZERO) "Excess" else "Short",
                    value = "₹${balance.abs()}",
                    color = if (balance >= BigDecimal.ZERO) Color(0xFFF9B500) else Color(0xFFFF6B6B),
                    modifier = Modifier.weight(1f)
                )
                showEfficiency -> {
                    val ratio = if (expense > BigDecimal.ZERO)
                        income.multiply(BigDecimal("100"))
                            .divide(expense, 0, java.math.RoundingMode.HALF_UP).toInt()
                    else if (income > BigDecimal.ZERO) 100 else 0
                    val isPositive = income >= expense
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Effic.",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.45f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp
                                              else Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isPositive) Color(0xFF69F0AE) else Color(0xFFFF5252)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = "$ratio%",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isPositive) Color(0xFF69F0AE) else Color(0xFFFF5252)
                            )
                        }
                    }
                }
                else -> Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatItemMini(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.45f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    color: Color,
    icon: ImageVector? = null
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

// ─── Quick actions ────────────────────────────────────────────────────────────

@Composable
private fun QuickActionsSection(
    onAddToday: () -> Unit,
    onAddForDate: () -> Unit,
    onReports: () -> Unit,
    onPayments: () -> Unit,
    onWeekly: () -> Unit,
    onExpenseTypes: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp
            )
            Spacer(Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                QuickActionButton(
                    label = "Add Today",
                    icon = Icons.Default.Today,
                    iconBgColor = Color(0xFF1D7A4D),
                    modifier = Modifier.weight(1f),
                    onClick = onAddToday
                )
                QuickActionButton(
                    label = "By Date",
                    icon = Icons.Default.CalendarMonth,
                    iconBgColor = Color(0xFF1565C0),
                    modifier = Modifier.weight(1f),
                    onClick = onAddForDate
                )
                QuickActionButton(
                    label = "Reports",
                    icon = Icons.Default.BarChart,
                    iconBgColor = Color(0xFF6A1B9A),
                    modifier = Modifier.weight(1f),
                    onClick = onReports
                )
            }

            Spacer(Modifier.height(4.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                QuickActionButton(
                    label = "Payments",
                    icon = Icons.Default.Payments,
                    iconBgColor = Color(0xFFBF6000),
                    modifier = Modifier.weight(1f),
                    onClick = onPayments
                )
                QuickActionButton(
                    label = "Weekly",
                    icon = Icons.Default.ListAlt,
                    iconBgColor = Color(0xFF00695C),
                    modifier = Modifier.weight(1f),
                    onClick = onWeekly
                )
                QuickActionButton(
                    label = "Breakdown",
                    icon = Icons.Default.PieChart,
                    iconBgColor = Color(0xFFC62828),
                    modifier = Modifier.weight(1f),
                    onClick = onExpenseTypes
                )
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    icon: ImageVector,
    iconBgColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

// ─── Month group header ───────────────────────────────────────────────────────

@Composable
private fun MonthGroupHeader(
    label: String,
    count: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = "$count entries",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─── Expense list card ────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExpenseListCard(
    expense: DailyExpense,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val hasReceipt = remember(expense.otherExpenses) {
        try {
            kotlinx.serialization.json.Json.decodeFromString(
                ListSerializer(OtherExpenseEntry.serializer()), expense.otherExpenses
            ).any { it.receiptImagePath != null }
        } catch (e: Exception) { false }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = try {
                            LocalDate.parse(expense.date)
                                .format(DateTimeFormatter.ofPattern("dd MMM yyyy, EEE"))
                        } catch (e: Exception) { expense.date },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (hasReceipt) {
                        Icon(
                            imageVector = Icons.Default.Attachment,
                            contentDescription = "Has attachment",
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    AmountChip(
                        label = "Inc",
                        value = "₹${expense.totalIncome}",
                        color = MaterialTheme.colorScheme.primary,
                        bgColor = MaterialTheme.colorScheme.primaryContainer
                    )
                    AmountChip(
                        label = "Exp",
                        value = "₹${expense.totalLaborCost + expense.totalOvertimeCost + expense.totalOtherExpensesCost}",
                        color = MaterialTheme.colorScheme.tertiary,
                        bgColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                    AmountChip(
                        label = "Pay",
                        value = "₹${expense.advanceAmount + expense.excessBalance + expense.weeklyPaymentDone}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        bgColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                val totalWorkers = expense.malayaliMaleCount + expense.bengaliMaleCount +
                    expense.malayaliFemaleCount + expense.bengaliFemaleCount
                if (totalWorkers > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "$totalWorkers workers (${expense.malayaliMaleCount}M + ${expense.bengaliMaleCount}M + ${expense.malayaliFemaleCount}F + ${expense.bengaliFemaleCount}F)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (expense.comments.isNotEmpty()) {
                    Text(
                        text = expense.comments,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun AmountChip(
    label: String,
    value: String,
    color: Color,
    bgColor: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = color.copy(alpha = 0.65f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun EmptyStateCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.EventNote,
                contentDescription = null,
                modifier = Modifier.size(52.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Text(
                text = "No expenses recorded yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onClick) {
                Text("Add Your First Expense")
            }
        }
    }
}

// ─── Expense detail dialog ────────────────────────────────────────────────────

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
                        LocalDate.parse(expense.date)
                            .format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        DetailRow("Total Labor", "₹${expense.totalLaborCost}")
                        DetailRow("Total Overtime", "₹${expense.totalOvertimeCost}")
                        DetailRow("Other Expenses", "₹${expense.totalOtherExpensesCost}")
                        DetailRow("Advances Paid", "₹${expense.advanceAmount}")
                        DetailRow("Weekly Settlement", "₹${expense.weeklyPaymentDone}")
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        DetailRow(
                            "TOTAL INCOME",
                            "₹${expense.totalIncome}",
                            isTotal = true,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (workerGroups.isNotEmpty()) {
                    SectionTitle("Labor Breakdown")
                    workerGroups.forEach { group ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp)
                        ) {
                            Text("${group.workerTypeName} (${group.count})", style = MaterialTheme.typography.bodyMedium)
                            Text("Task: ${group.taskPerformed}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                            Text("Cost: ₹${group.calculateTotalGroupCost()}", style = MaterialTheme.typography.bodySmall)
                            if (group.comments.isNotEmpty()) {
                                Text(group.comments, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                        }
                    }
                }

                if (otherExpenses.isNotEmpty()) {
                    SectionTitle("Other Expenses")
                    otherExpenses.forEach { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                val titleText = if (entry.subtypeName != null)
                                    "${entry.typeName} (${entry.subtypeName})"
                                else entry.typeName
                                Text(text = titleText, style = MaterialTheme.typography.bodyMedium)
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
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { onImageClick(entry.receiptImagePath) },
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    }
                }

                if (incomeEntries.isNotEmpty()) {
                    SectionTitle("Income Details")
                    incomeEntries.forEach { entry ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp)
                        ) {
                            Text(entry.typeName, style = MaterialTheme.typography.bodyMedium)
                            Text("${entry.weight} kg @ ₹${entry.pricePerKilo}/kg", style = MaterialTheme.typography.bodySmall)
                            Text("Total: ₹${entry.amount}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                        }
                    }
                }

                if (advanceEntries.isNotEmpty()) {
                    SectionTitle("Advance Payments")
                    advanceEntries.forEach { entry ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp)
                        ) {
                            Text("Recipient: ${entry.recipientName}", style = MaterialTheme.typography.bodyMedium)
                            Text("Amount: ₹${entry.amount}", style = MaterialTheme.typography.bodySmall)
                            if (entry.reason.isNotEmpty()) {
                                Text("Reason: ${entry.reason}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
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
private fun DetailRow(
    label: String,
    value: String,
    isTotal: Boolean = false,
    color: Color = Color.Unspecified
) {
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
            color = if (color != Color.Unspecified) color
                    else if (isTotal) MaterialTheme.colorScheme.primary
                    else Color.Unspecified
        )
    }
}

// ─── Clone expense dialog ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloneExpenseDialog(
    expense: DailyExpense,
    nextWorkday: LocalDate,
    onDismiss: () -> Unit,
    onClone: (LocalDate) -> Unit
) {
    val sourceLabel = try {
        LocalDate.parse(expense.date).format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
    } catch (_: Exception) { expense.date }

    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = nextWorkday
                .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val chosen = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                        onClone(chosen)
                    }
                    showDatePicker = false
                }) { Text("Clone") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
        title = { Text("Clone Expense") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Copy all labor, other expenses, and comments from $sourceLabel to:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Overtime, other expenses, income, advances, and settlements are NOT copied.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = { onClone(nextWorkday) }) {
                Icon(Icons.Default.FastForward, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(nextWorkday.format(DateTimeFormatter.ofPattern("EEE dd MMM")))
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pick Date")
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

// ─── Image preview dialog ─────────────────────────────────────────────────────

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
