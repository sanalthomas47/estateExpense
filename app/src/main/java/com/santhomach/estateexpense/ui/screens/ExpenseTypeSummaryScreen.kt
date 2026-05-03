package com.santhomach.estateexpense.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.santhomach.estateexpense.ui.viewmodel.ExpenseTypeRow
import com.santhomach.estateexpense.ui.viewmodel.ExpenseTypeSummaryViewModel
import com.santhomach.estateexpense.ui.viewmodel.SubtypeRow
import com.santhomach.estateexpense.ui.viewmodel.TrendDirection
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseTypeSummaryScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: ExpenseTypeSummaryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expense Breakdown") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Error: ${uiState.error}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            uiState.rows.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No expense type data found.\nAdd some other expenses to see the breakdown.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    // Sticky column header
                    item(key = "header") {
                        ColumnHeader()
                    }

                    HorizontalDividerItem()

                    // Type rows
                    items(uiState.rows, key = { it.typeName }) { row ->
                        TypeRow(
                            row = row,
                            onToggle = { viewModel.toggleExpanded(row.typeName) }
                        )

                        AnimatedVisibility(
                            visible = row.isExpanded,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Column {
                                row.subtypes.forEach { sub ->
                                    SubtypeRowItem(
                                        key = "${row.typeName}-${sub.subtypeName}",
                                        subtype = sub
                                    )
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }

                    // Grand total footer
                    item(key = "footer") {
                        HorizontalDivider(thickness = 1.5.dp)
                        TotalRow(
                            thisWeekTotal = uiState.thisWeekTotal,
                            thisYearTotal = uiState.thisYearTotal,
                            allTimeTotal = uiState.allTimeTotal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Expense Type",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(2.2f)
        )
        HeaderCell("This Week", Modifier.weight(1.3f))
        HeaderCell("This Year", Modifier.weight(1.3f))
        HeaderCell("All Time", Modifier.weight(1.3f))
    }
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.End,
        modifier = modifier
    )
}

@Composable
private fun TypeRow(
    row: ExpenseTypeRow,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Expand icon — only show if there are subtypes
        val hasSubtypes = row.subtypes.isNotEmpty()
        if (hasSubtypes) {
            Icon(
                imageVector = if (row.isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (row.isExpanded) "Collapse" else "Expand",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(4.dp))
        } else {
            Spacer(modifier = Modifier.width(20.dp))
        }

        Text(
            text = row.typeName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(2f)
        )

        AmountCell(
            amount = row.thisWeekAmount,
            percent = row.thisWeekPercent,
            trend = row.weekTrend,
            modifier = Modifier.weight(1.3f)
        )
        AmountCell(
            amount = row.thisYearAmount,
            percent = row.thisYearPercent,
            trend = row.yearTrend,
            modifier = Modifier.weight(1.3f)
        )
        AmountCell(
            amount = row.allTimeAmount,
            percent = row.allTimePercent,
            trend = TrendDirection.NEUTRAL,
            showTrend = false,
            modifier = Modifier.weight(1.3f)
        )
    }
}

@Composable
private fun SubtypeRowItem(
    key: String,
    subtype: SubtypeRow
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(start = 36.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "└ ${subtype.subtypeName.ifBlank { "Unspecified" }}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(2f)
        )
        SimpleAmountCell(subtype.thisWeekAmount, Modifier.weight(1.3f))
        SimpleAmountCell(subtype.thisYearAmount, Modifier.weight(1.3f))
        SimpleAmountCell(subtype.allTimeAmount, Modifier.weight(1.3f))
    }
}

@Composable
private fun TotalRow(
    thisWeekTotal: BigDecimal,
    thisYearTotal: BigDecimal,
    allTimeTotal: BigDecimal
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(20.dp))
        Text(
            text = "TOTAL",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(2f)
        )
        TotalCell(thisWeekTotal, Modifier.weight(1.3f))
        TotalCell(thisYearTotal, Modifier.weight(1.3f))
        TotalCell(allTimeTotal, Modifier.weight(1.3f))
    }
}

@Composable
private fun AmountCell(
    amount: BigDecimal,
    percent: Float,
    trend: TrendDirection,
    showTrend: Boolean = true,
    modifier: Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showTrend && trend != TrendDirection.NEUTRAL) {
                Icon(
                    imageVector = if (trend == TrendDirection.UP)
                        Icons.AutoMirrored.Filled.TrendingUp
                    else
                        Icons.AutoMirrored.Filled.TrendingDown,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = if (trend == TrendDirection.UP) Color(0xFFD32F2F) else Color(0xFF388E3C)
                )
                Spacer(modifier = Modifier.width(2.dp))
            }
            Text(
                text = "₹${formatAmount(amount)}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.End,
                maxLines = 1
            )
        }
        if (percent > 0f) {
            Text(
                text = "${String.format("%.1f", percent)}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun SimpleAmountCell(amount: BigDecimal, modifier: Modifier) {
    Text(
        text = if (amount == BigDecimal.ZERO) "—" else "₹${formatAmount(amount)}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.End,
        maxLines = 1,
        modifier = modifier
    )
}

@Composable
private fun TotalCell(amount: BigDecimal, modifier: Modifier) {
    Text(
        text = "₹${formatAmount(amount)}",
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.End,
        maxLines = 1,
        modifier = modifier
    )
}

// Inserts a divider as a lazy list item
private fun androidx.compose.foundation.lazy.LazyListScope.HorizontalDividerItem() {
    item(key = "divider_header") {
        HorizontalDivider(thickness = 1.5.dp)
    }
}

private fun formatAmount(amount: BigDecimal): String {
    if (amount == BigDecimal.ZERO) return "0"
    val long = amount.toLong()
    return when {
        long >= 100_000 -> "${String.format("%.1f", long / 100_000.0)}L"
        long >= 1_000 -> "${String.format("%.1f", long / 1_000.0)}K"
        else -> amount.stripTrailingZeros().toPlainString()
    }
}
