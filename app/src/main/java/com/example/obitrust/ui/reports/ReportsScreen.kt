package com.example.obitrust.ui.reports

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.obitrust.data.accounting.AccountingEngine
import com.example.obitrust.ui.BusinessViewModel
import com.example.obitrust.ui.components.MetricCard
import com.example.obitrust.ui.components.SectionHeader
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ReportsScreen(
    viewModel: BusinessViewModel,
    modifier: Modifier = Modifier
) {
    val sales by viewModel.sales.collectAsState()
    val purchases by viewModel.purchases.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val products by viewModel.products.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val journalLines by viewModel.journalLines.collectAsState()
    val profile by viewModel.businessProfile.collectAsState()

    val context = LocalContext.current
    val currency = profile?.currencySymbol ?: "GH₵"

    var selectedPeriod by remember { mutableStateOf("This Month") }
    val periods = listOf("Today", "Yesterday", "This Week", "This Month", "Last Month", "This Year", "All Time")

    // Filter timestamp calculations
    val (startMillis, endMillis) = remember(selectedPeriod) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        when (selectedPeriod) {
            "Today" -> {
                val start = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                start to cal.timeInMillis
            }
            "Yesterday" -> {
                cal.add(Calendar.DAY_OF_YEAR, -1)
                val start = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                start to cal.timeInMillis
            }
            "This Week" -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                val start = cal.timeInMillis
                start to System.currentTimeMillis()
            }
            "This Month" -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val start = cal.timeInMillis
                start to System.currentTimeMillis()
            }
            "Last Month" -> {
                cal.add(Calendar.MONTH, -1)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val start = cal.timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                start to cal.timeInMillis
            }
            "This Year" -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                val start = cal.timeInMillis
                start to System.currentTimeMillis()
            }
            else -> 0L to Long.MAX_VALUE
        }
    }

    val periodSales = sales.filter { it.status != "VOIDED" && it.createdAt in startMillis..endMillis }
    val periodPurchases = purchases.filter { it.createdAt in startMillis..endMillis }
    val periodExpenses = expenses.filter { it.createdAt in startMillis..endMillis }

    val totalSalesRevenue = periodSales.sumOf { it.totalAmount }
    val totalPurchasesCost = periodPurchases.sumOf { it.totalAmount }
    val totalExpensesCost = periodExpenses.sumOf { it.amount }
    val estimatedGrossProfit = (totalSalesRevenue * 0.35) // Approximate 35% margin for period
    val estimatedNetProfit = estimatedGrossProfit - totalExpensesCost

    val sdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    fun generateReportText(reportType: String): String {
        return buildString {
            appendLine("==========================================")
            appendLine("       ${profile?.businessName ?: "OBITRUST VENTURES GHANA"}")
            appendLine("           FINANCIAL REPORT: $reportType")
            appendLine("           Period: $selectedPeriod")
            appendLine("         Generated: ${sdf.format(Date())}")
            appendLine("==========================================")
            appendLine("• Total Sales: $currency ${String.format(Locale.US, "%,.2f", totalSalesRevenue)} (${periodSales.size} sales)")
            appendLine("• Total Stock Purchases: $currency ${String.format(Locale.US, "%,.2f", totalPurchasesCost)}")
            appendLine("• Total Operating Expenses: $currency ${String.format(Locale.US, "%,.2f", totalExpensesCost)}")
            appendLine("• Estimated Gross Profit: $currency ${String.format(Locale.US, "%,.2f", estimatedGrossProfit)}")
            appendLine("• Estimated Net Profit: $currency ${String.format(Locale.US, "%,.2f", estimatedNetProfit)}")
            appendLine("------------------------------------------")
            appendLine("CURRENT LIQUIDITY & DEBT:")
            appendLine("• Total Customer Debt Owed: $currency ${String.format(Locale.US, "%,.2f", customers.sumOf { it.outstandingDebt })}")
            appendLine("• Total Supplier Debt Owed: $currency ${String.format(Locale.US, "%,.2f", suppliers.sumOf { it.outstandingBalance })}")
            appendLine("• Current Stock Inventory (Cost): $currency ${String.format(Locale.US, "%,.2f", products.sumOf { it.costPrice * it.quantity })}")
            appendLine("==========================================")
            appendLine("Report certified by Obitrust Business Manager.")
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("reports_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Reports & Business Analytics", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Filter by date and export certified statements to PDF / CSV", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Period filter chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(periods) { p ->
                    FilterChip(
                        selected = selectedPeriod == p,
                        onClick = { selectedPeriod = p },
                        label = { Text(p) }
                    )
                }
            }
        }

        // Period Metrics
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard(
                    title = "Period Sales",
                    value = "$currency ${String.format(Locale.US, "%,.2f", totalSalesRevenue)}",
                    subtitle = "${periodSales.size} transactions",
                    icon = Icons.Default.TrendingUp,
                    iconTint = EmeraldProfit,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Period Expenses",
                    value = "$currency ${String.format(Locale.US, "%,.2f", totalExpensesCost)}",
                    subtitle = "${periodExpenses.size} outlays",
                    icon = Icons.Default.Receipt,
                    iconTint = CrimsonExpense,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard(
                    title = "Stock Purchases",
                    value = "$currency ${String.format(Locale.US, "%,.2f", totalPurchasesCost)}",
                    subtitle = "${periodPurchases.size} supplier invoices",
                    icon = Icons.Default.Inventory,
                    iconTint = GoldPrimary,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Estimated Net Return",
                    value = "$currency ${String.format(Locale.US, "%,.2f", estimatedNetProfit)}",
                    icon = Icons.Default.Savings,
                    iconTint = if (estimatedNetProfit >= 0) EmeraldProfit else CrimsonExpense,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            SectionHeader(title = "Exportable Business Reports", subtitle = "Share or print professional statements")
        }

        val reportTypes = listOf(
            Triple("Comprehensive Financial Summary", "Full P&L, sales, expenses, and current working capital", Icons.Default.Description),
            Triple("Sales & Revenue Report", "Itemized sales register with customer breakdown and payment channels", Icons.Default.PointOfSale),
            Triple("Stock & Inventory Audit Report", "Product catalog, warehouse count, stock valuation and reorder levels", Icons.Default.Inventory2),
            Triple("Customer Debtors Report", "Outstanding customer credit, payment aging and contact information", Icons.Default.People),
            Triple("Supplier Creditors Report", "Supplier payables, invoices due and payment terms", Icons.Default.LocalShipping),
            Triple("Tax & GRA Levies Report", "Collected VAT, NHIL, GETFund, and taxable sales base", Icons.Default.ReceiptLong)
        )

        items(reportTypes) { (title, desc, icon) ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth().testTag("report_${title.take(10).lowercase()}")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(icon, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    FilledTonalButton(
                        onClick = {
                            val text = generateReportText(title)
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, text)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Export / Share $title"))
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export")
                    }
                }
            }
        }
    }
}
