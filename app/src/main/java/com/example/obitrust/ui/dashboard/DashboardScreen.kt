package com.example.obitrust.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.obitrust.data.accounting.AccountingEngine
import com.example.obitrust.ui.BusinessViewModel
import com.example.obitrust.ui.NavigationScreen
import com.example.obitrust.ui.components.BusinessLogo
import com.example.obitrust.ui.components.MetricCard
import com.example.obitrust.ui.components.SectionHeader
import com.example.obitrust.ui.components.SimpleBarChart
import com.example.obitrust.ui.components.SimpleDonutChart
import com.example.ui.theme.*
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: BusinessViewModel,
    modifier: Modifier = Modifier
) {
    val sales by viewModel.sales.collectAsState()
    val purchases by viewModel.purchases.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val products by viewModel.products.collectAsState()
    val lowStock by viewModel.lowStockProducts.collectAsState()
    val outOfStock by viewModel.outOfStockProducts.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val journalLines by viewModel.journalLines.collectAsState()
    val profile by viewModel.businessProfile.collectAsState()

    val currency = profile?.currencySymbol ?: "GH₵"

    // Time calculations
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val todayStart = cal.timeInMillis

    cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
    val weekStart = cal.timeInMillis

    cal.set(Calendar.DAY_OF_MONTH, 1)
    val monthStart = cal.timeInMillis

    val validSales = sales.filter { it.status != "VOIDED" }
    val salesToday = validSales.filter { it.createdAt >= todayStart }.sumOf { it.totalAmount }
    val salesWeek = validSales.filter { it.createdAt >= weekStart }.sumOf { it.totalAmount }
    val salesMonth = validSales.filter { it.createdAt >= monthStart }.sumOf { it.totalAmount }

    val totalPurchases = purchases.sumOf { it.totalAmount }
    val totalExpenses = expenses.sumOf { it.amount }

    // Double-entry Profit & Loss
    val pl = remember(journalLines) { AccountingEngine.generateProfitLoss(journalLines) }

    // Inventory & Debt
    val currentStockCostValue = products.sumOf { it.costPrice * it.quantity }
    val inStockProductCount = products.filter { it.quantity > 0 }.size
    val totalCustomerDebt = customers.sumOf { it.outstandingDebt }
    val totalSupplierDebt = suppliers.sumOf { it.outstandingBalance }

    // Accounts
    val cashAccounts = accounts.filter { it.accountType == "CASH" }.sumOf { it.balance }
    val bankAccounts = accounts.filter { it.accountType == "BANK" }.sumOf { it.balance }
    val momoAccounts = accounts.filter { it.accountType == "MOBILE_MONEY" }.sumOf { it.balance }

    // Daily Sales Bar Chart Data (last 7 days)
    val dailyChartData = remember(sales) {
        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        days.mapIndexed { idx, day ->
            // Distribute sample/live sales across the days
            val daySales = validSales.filter {
                val c = Calendar.getInstance().apply { timeInMillis = it.createdAt }
                val dayOfWeek = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Monday=0
                dayOfWeek == idx
            }.sumOf { it.totalAmount }
            day to daySales
        }
    }

    // Expense Categories Donut Data
    val expenseChartData = remember(expenses) {
        expenses.groupBy { it.category }
            .map { (cat, list) -> cat to list.sumOf { it.amount } }
            .sortedByDescending { it.second }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome & Business Banner
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavyBorder)),
                modifier = Modifier.fillMaxWidth().testTag("business_header_card")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BusinessLogo(
                        logoUri = profile?.logoUri,
                        selectedPreset = profile?.selectedLogoPreset ?: "SHIELD",
                        size = 48.dp,
                        modifier = Modifier.testTag("dashboard_business_logo")
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile?.businessName ?: "OBITRUST VENTURES GHANA",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = profile?.tagline ?: "Business Management & Accounting",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    AssistChip(
                        onClick = { viewModel.setScreen(NavigationScreen.AI_ACCOUNTANT) },
                        label = { Text("AI Active", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(14.dp))
                        },
                        colors = AssistChipDefaults.assistChipColors(containerColor = NavyCard, labelColor = GoldPrimary)
                    )
                }
            }
        }

        // Low & Out of Stock Notification Banners
        if (outOfStock.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CrimsonExpense.copy(alpha = 0.15f)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CrimsonExpense)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setScreen(NavigationScreen.INVENTORY) }
                        .testTag("out_of_stock_alert")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = CrimsonExpense)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Critical: ${outOfStock.size} Product(s) Out of Stock!",
                                fontWeight = FontWeight.Bold,
                                color = CrimsonExpense,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "Tap to view depleted inventory & create purchase orders",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (lowStock.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF59E0B).copy(alpha = 0.15f)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFF59E0B))),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setScreen(NavigationScreen.INVENTORY) }
                        .testTag("low_stock_alert")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.WarningAmber, contentDescription = null, tint = Color(0xFFF59E0B))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Reorder Notice: ${lowStock.size} Product(s) Below Minimum Stock",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF59E0B),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "Safety levels reached. Reorder soon to maintain sales.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Section: Sales Performance KPIs
        item {
            SectionHeader(
                title = "Sales & Profitability",
                subtitle = "Automated double-entry metrics",
                actionText = "POS Cashier",
                onActionClick = { viewModel.setScreen(NavigationScreen.SALES) }
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Sales Today",
                    value = "$currency ${formatMoney(salesToday)}",
                    subtitle = "This Week: $currency ${formatMoney(salesWeek)}",
                    icon = Icons.Default.PointOfSale,
                    iconTint = EmeraldProfit,
                    modifier = Modifier.weight(1f),
                    testTag = "kpi_sales_today",
                    onClick = { viewModel.setScreen(NavigationScreen.SALES) }
                )
                MetricCard(
                    title = "Sales This Month",
                    value = "$currency ${formatMoney(salesMonth)}",
                    subtitle = "${validSales.size} Total Sales",
                    icon = Icons.Default.TrendingUp,
                    iconTint = GoldPrimary,
                    modifier = Modifier.weight(1f),
                    testTag = "kpi_sales_month",
                    onClick = { viewModel.setScreen(NavigationScreen.SALES) }
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Gross Profit",
                    value = "$currency ${formatMoney(pl.grossProfit)}",
                    subtitle = "Margin: ${formatPercent(pl.grossMarginPercent)}",
                    icon = Icons.Default.ShowChart,
                    iconTint = EmeraldProfit,
                    modifier = Modifier.weight(1f),
                    testTag = "kpi_gross_profit",
                    onClick = { viewModel.setScreen(NavigationScreen.ACCOUNTING) }
                )
                MetricCard(
                    title = "Net Profit",
                    value = "$currency ${formatMoney(pl.netProfit)}",
                    subtitle = "Expenses: $currency ${formatMoney(pl.totalExpenses)}",
                    icon = Icons.Default.Savings,
                    iconTint = if (pl.netProfit >= 0) EmeraldProfit else CrimsonExpense,
                    modifier = Modifier.weight(1f),
                    testTag = "kpi_net_profit",
                    onClick = { viewModel.setScreen(NavigationScreen.ACCOUNTING) }
                )
            }
        }

        // Section: Treasury & Multi-Payment Balances (Ghana MoMo, Cash, Bank)
        item {
            SectionHeader(
                title = "Treasury & Balances",
                subtitle = "Cash drawer, Bank, and Ghana Mobile Money",
                actionText = "Transfer",
                onActionClick = { viewModel.setScreen(NavigationScreen.ACCOUNTS) }
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard(
                    title = "Cash Drawer",
                    value = "$currency ${formatMoney(cashAccounts)}",
                    icon = Icons.Default.Payments,
                    iconTint = EmeraldProfit,
                    modifier = Modifier.weight(1f),
                    testTag = "balance_cash",
                    onClick = { viewModel.setScreen(NavigationScreen.ACCOUNTS) }
                )
                MetricCard(
                    title = "Bank Account",
                    value = "$currency ${formatMoney(bankAccounts)}",
                    icon = Icons.Default.AccountBalance,
                    iconTint = Color(0xFF38BDF8),
                    modifier = Modifier.weight(1f),
                    testTag = "balance_bank",
                    onClick = { viewModel.setScreen(NavigationScreen.ACCOUNTS) }
                )
                MetricCard(
                    title = "Mobile Money",
                    value = "$currency ${formatMoney(momoAccounts)}",
                    icon = Icons.Default.PhoneIphone,
                    iconTint = MtnYellow,
                    modifier = Modifier.weight(1f),
                    testTag = "balance_momo",
                    onClick = { viewModel.setScreen(NavigationScreen.ACCOUNTS) }
                )
            }
        }

        // Section: Stock & Debts
        item {
            SectionHeader(
                title = "Inventory & Working Capital",
                subtitle = "Stock valuation, customer debts & supplier obligations"
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Stock Value (Cost)",
                    value = "$currency ${formatMoney(currentStockCostValue)}",
                    subtitle = "$inStockProductCount products in stock",
                    icon = Icons.Default.Inventory2,
                    iconTint = GoldPrimary,
                    modifier = Modifier.weight(1f),
                    testTag = "kpi_stock_value",
                    onClick = { viewModel.setScreen(NavigationScreen.INVENTORY) }
                )
                MetricCard(
                    title = "Customer Debts (AR)",
                    value = "$currency ${formatMoney(totalCustomerDebt)}",
                    subtitle = "${customers.filter { it.outstandingDebt > 0 }.size} Debtors",
                    icon = Icons.Default.PersonSearch,
                    iconTint = CrimsonExpense,
                    modifier = Modifier.weight(1f),
                    testTag = "kpi_customer_debt",
                    onClick = { viewModel.setScreen(NavigationScreen.CUSTOMERS) }
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Supplier Debts (AP)",
                    value = "$currency ${formatMoney(totalSupplierDebt)}",
                    subtitle = "Purchases: $currency ${formatMoney(totalPurchases)}",
                    icon = Icons.Default.LocalShipping,
                    iconTint = Color(0xFFFB923C),
                    modifier = Modifier.weight(1f),
                    testTag = "kpi_supplier_debt",
                    onClick = { viewModel.setScreen(NavigationScreen.SUPPLIERS) }
                )
                MetricCard(
                    title = "Total Expenses",
                    value = "$currency ${formatMoney(totalExpenses)}",
                    subtitle = "${expenses.size} expense entries",
                    icon = Icons.Default.Receipt,
                    iconTint = CrimsonExpense,
                    modifier = Modifier.weight(1f),
                    testTag = "kpi_expenses",
                    onClick = { viewModel.setScreen(NavigationScreen.EXPENSES) }
                )
            }
        }

        // Section: Charts
        item {
            SectionHeader(
                title = "Daily Sales Performance",
                subtitle = "Revenue trends across the week"
            )
            SimpleBarChart(data = dailyChartData, barColor = GoldPrimary)
        }

        if (expenseChartData.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Expense Distribution",
                    subtitle = "Expenditure by category"
                )
                SimpleDonutChart(categories = expenseChartData)
            }
        }

        // Quick Navigation Grid
        item {
            SectionHeader(
                title = "Quick Actions",
                subtitle = "Direct shortcuts to all business operations"
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionCard(
                    title = "New Sale",
                    icon = Icons.Default.ShoppingCart,
                    color = EmeraldProfit,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.setScreen(NavigationScreen.SALES) }
                )
                QuickActionCard(
                    title = "Purchase Stock",
                    icon = Icons.Default.AddBusiness,
                    color = GoldPrimary,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.setScreen(NavigationScreen.PURCHASES) }
                )
                QuickActionCard(
                    title = "Log Expense",
                    icon = Icons.Default.MoneyOff,
                    color = CrimsonExpense,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.setScreen(NavigationScreen.EXPENSES) }
                )
                QuickActionCard(
                    title = "AI Accountant",
                    icon = Icons.Default.AutoAwesome,
                    color = Color(0xFFA855F7),
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.setScreen(NavigationScreen.AI_ACCOUNTANT) }
                )
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier
            .clickable { onClick() }
            .testTag("quick_action_${title.lowercase().replace(" ", "_")}")
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

private fun formatMoney(amount: Double): String {
    return String.format(Locale.US, "%,.2f", amount)
}

private fun formatPercent(value: Double): String {
    return String.format(Locale.US, "%.1f%%", value)
}
