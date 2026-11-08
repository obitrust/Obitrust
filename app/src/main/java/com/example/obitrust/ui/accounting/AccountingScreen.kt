package com.example.obitrust.ui.accounting

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.obitrust.data.accounting.AccountingEngine
import com.example.obitrust.data.accounting.ChartOfAccounts
import com.example.obitrust.data.model.JournalEntryEntity
import com.example.obitrust.data.model.JournalLineEntity
import com.example.obitrust.ui.BusinessViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountingScreen(
    viewModel: BusinessViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("General Ledger", "Trial Balance", "Profit & Loss", "Balance Sheet", "Cash Flow", "Accounts Directory")

    val journalEntries by viewModel.journalEntries.collectAsState()
    val journalLines by viewModel.journalLines.collectAsState()
    val products by viewModel.products.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val profile by viewModel.businessProfile.collectAsState()

    val currency = profile?.currencySymbol ?: "GH₵"

    val trialBalance = remember(journalLines) { AccountingEngine.generateTrialBalance(journalLines) }
    val pl = remember(journalLines) { AccountingEngine.generateProfitLoss(journalLines) }

    val stockCost = products.sumOf { it.costPrice * it.quantity }
    val cashTotal = accounts.sumOf { it.balance }
    val arTotal = customers.sumOf { it.outstandingDebt }
    val apTotal = suppliers.sumOf { it.outstandingBalance }

    val balanceSheet = remember(journalLines, stockCost, cashTotal, arTotal, apTotal) {
        AccountingEngine.generateBalanceSheet(journalLines, stockCost, cashTotal, arTotal, apTotal)
    }

    val cashFlow = remember(journalLines) {
        val inflowSales = journalLines.filter { it.accountCode.startsWith("10") && it.debit > 0 }.sumOf { it.debit }
        val inflowDebts = journalLines.filter { it.accountCode == ChartOfAccounts.CODE_ACCOUNTS_RECEIVABLE && it.credit > 0 }.sumOf { it.credit }
        val outflowPurchases = journalLines.filter { it.accountCode == ChartOfAccounts.CODE_INVENTORY && it.debit > 0 }.sumOf { it.debit }
        val outflowExpenses = journalLines.filter { it.accountCode.startsWith("6") }.sumOf { it.debit }
        val outflowSuppliers = journalLines.filter { it.accountCode == ChartOfAccounts.CODE_ACCOUNTS_PAYABLE && it.debit > 0 }.sumOf { it.debit }
        AccountingEngine.generateCashFlow(inflowSales, inflowDebts, outflowPurchases, outflowExpenses, outflowSuppliers)
    }

    Column(modifier = modifier.fillMaxSize().testTag("accounting_screen")) {
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = NavySurface,
            contentColor = GoldPrimary,
            edgePadding = 12.dp
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("accounting_tab_$index")
                )
            }
        }

        when (selectedTab) {
            0 -> GeneralLedgerTab(journalEntries = journalEntries, journalLines = journalLines, currency = currency)
            1 -> TrialBalanceTab(summary = trialBalance, currency = currency)
            2 -> ProfitLossTab(pl = pl, currency = currency)
            3 -> BalanceSheetTab(bs = balanceSheet, currency = currency)
            4 -> CashFlowTab(cf = cashFlow, currency = currency)
            5 -> ChartOfAccountsTab()
        }
    }
}

@Composable
private fun GeneralLedgerTab(
    journalEntries: List<JournalEntryEntity>,
    journalLines: List<JournalLineEntity>,
    currency: String
) {
    var selectedTypeFilter by remember { mutableStateOf("ALL") }
    val filters = listOf("ALL", "SALE", "PURCHASE", "EXPENSE", "CUSTOMER_PAYMENT", "SUPPLIER_PAYMENT", "TRANSFER", "ADJUSTMENT")

    val sdf = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    val filteredEntries = remember(journalEntries, selectedTypeFilter) {
        if (selectedTypeFilter == "ALL") journalEntries else journalEntries.filter { it.transactionType == selectedTypeFilter }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(filters) { f ->
                FilterChip(
                    selected = selectedTypeFilter == f,
                    onClick = { selectedTypeFilter = f },
                    label = { Text(f) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (filteredEntries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No journal entries found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredEntries) { entry ->
                    val lines = journalLines.filter { it.journalEntryId == entry.id }

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth().testTag("journal_entry_${entry.id}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(entry.entryNumber, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Badge(containerColor = GoldPrimary.copy(alpha = 0.2f), contentColor = GoldPrimary) {
                                        Text(entry.transactionType, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text(sdf.format(Date(entry.dateMillis)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Text(entry.description, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 4.dp))

                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                            // Double entry lines
                            lines.forEach { line ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "${line.accountCode} - ${line.accountName}",
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (line.debit > 0) {
                                        Text("Dr: $currency ${String.format(Locale.US, "%.2f", line.debit)}", color = EmeraldProfit, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                    }
                                    if (line.credit > 0) {
                                        Text("Cr: $currency ${String.format(Locale.US, "%.2f", line.credit)}", color = GoldPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                    }
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
private fun TrialBalanceTab(
    summary: com.example.obitrust.data.accounting.TrialBalanceSummary,
    currency: String
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Balance Audit Banner
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (summary.isBalanced) EmeraldProfit.copy(alpha = 0.15f) else CrimsonExpense.copy(alpha = 0.15f)
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(if (summary.isBalanced) EmeraldProfit else CrimsonExpense)
                ),
                modifier = Modifier.fillMaxWidth().testTag("trial_balance_banner")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (summary.isBalanced) Icons.Default.Verified else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (summary.isBalanced) EmeraldProfit else CrimsonExpense,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (summary.isBalanced) "PERFECTLY BALANCED LEDGER" else "UNBALANCED AUDIT WARNING",
                            fontWeight = FontWeight.Bold,
                            color = if (summary.isBalanced) EmeraldProfit else CrimsonExpense,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Total Debits: $currency ${String.format(Locale.US, "%,.2f", summary.totalDebit)} | Total Credits: $currency ${String.format(Locale.US, "%,.2f", summary.totalCredit)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        item {
            Text("Trial Balance Sheet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        // Table Header
        item {
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Account", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(2f))
                    Text("Debit ($currency)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Text("Credit ($currency)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                }
            }
        }

        items(summary.rows) { row ->
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth().testTag("tb_row_${row.accountCode}")
            ) {
                Row(
                    modifier = Modifier.padding(10.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(2f)) {
                        Text("${row.accountCode} - ${row.accountName}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        if (row.netDebit > 0) String.format(Locale.US, "%,.2f", row.netDebit) else "-",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                        color = EmeraldProfit
                    )
                    Text(
                        if (row.netCredit > 0) String.format(Locale.US, "%,.2f", row.netCredit) else "-",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                        color = GoldPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfitLossTab(
    pl: com.example.obitrust.data.accounting.ProfitLossStatement,
    currency: String
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                modifier = Modifier.fillMaxWidth().testTag("pl_net_profit_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Net Business Profit", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "$currency ${String.format(Locale.US, "%,.2f", pl.netProfit)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (pl.netProfit >= 0) EmeraldProfit else CrimsonExpense
                    )
                    Text(
                        "Net Margin: ${String.format(Locale.US, "%.1f%%", pl.netMarginPercent)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Revenue & Gross Margin", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Sales Revenue (4010):", style = MaterialTheme.typography.bodySmall)
                        Text("$currency ${String.format(Locale.US, "%,.2f", pl.salesRevenue)}", fontWeight = FontWeight.SemiBold)
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Cost of Goods Sold (5010):", style = MaterialTheme.typography.bodySmall)
                        Text("-$currency ${String.format(Locale.US, "%,.2f", pl.costOfGoodsSold)}", color = CrimsonExpense)
                    }
                    HorizontalDivider()
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Gross Profit:", fontWeight = FontWeight.Bold)
                        Text("$currency ${String.format(Locale.US, "%,.2f", pl.grossProfit)}", fontWeight = FontWeight.Bold, color = EmeraldProfit)
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Operating Expenses", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    pl.operatingExpenses.forEach { (cat, amt) ->
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(cat, style = MaterialTheme.typography.bodySmall)
                            Text("$currency ${String.format(Locale.US, "%,.2f", amt)}", color = CrimsonExpense)
                        }
                    }
                    HorizontalDivider()
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Total Operating Expenses:", fontWeight = FontWeight.Bold)
                        Text("$currency ${String.format(Locale.US, "%,.2f", pl.totalExpenses)}", fontWeight = FontWeight.Bold, color = CrimsonExpense)
                    }
                }
            }
        }
    }
}

@Composable
private fun BalanceSheetTab(
    bs: com.example.obitrust.data.accounting.BalanceSheet,
    currency: String
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                modifier = Modifier.fillMaxWidth().testTag("balance_sheet_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Statement of Financial Position (Balance Sheet)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("Assets = Liabilities + Owner's Equity", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Assets
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Current Assets", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = EmeraldProfit)
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Cash & Bank & MoMo:", style = MaterialTheme.typography.bodySmall)
                        Text("$currency ${String.format(Locale.US, "%,.2f", bs.cashAndEquivalents)}")
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Accounts Receivable (Debtors):", style = MaterialTheme.typography.bodySmall)
                        Text("$currency ${String.format(Locale.US, "%,.2f", bs.accountsReceivable)}")
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Merchandise Inventory Asset:", style = MaterialTheme.typography.bodySmall)
                        Text("$currency ${String.format(Locale.US, "%,.2f", bs.merchandiseInventory)}")
                    }
                    HorizontalDivider()
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Total Assets:", fontWeight = FontWeight.Bold)
                        Text("$currency ${String.format(Locale.US, "%,.2f", bs.totalAssets)}", fontWeight = FontWeight.Bold, color = EmeraldProfit)
                    }
                }
            }
        }

        // Liabilities
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Current Liabilities", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = CrimsonExpense)
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Accounts Payable (Suppliers):", style = MaterialTheme.typography.bodySmall)
                        Text("$currency ${String.format(Locale.US, "%,.2f", bs.accountsPayable)}")
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Sales Tax / VAT Payable:", style = MaterialTheme.typography.bodySmall)
                        Text("$currency ${String.format(Locale.US, "%,.2f", bs.taxPayable)}")
                    }
                    HorizontalDivider()
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Total Liabilities:", fontWeight = FontWeight.Bold)
                        Text("$currency ${String.format(Locale.US, "%,.2f", bs.totalLiabilities)}", fontWeight = FontWeight.Bold, color = CrimsonExpense)
                    }
                }
            }
        }

        // Equity
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Owner's Equity", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = GoldPrimary)
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Owner's Capital:", style = MaterialTheme.typography.bodySmall)
                        Text("$currency ${String.format(Locale.US, "%,.2f", bs.ownerCapital)}")
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Current Period Net Profit:", style = MaterialTheme.typography.bodySmall)
                        Text("$currency ${String.format(Locale.US, "%,.2f", bs.currentPeriodNetProfit)}")
                    }
                    HorizontalDivider()
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Total Equity:", fontWeight = FontWeight.Bold)
                        Text("$currency ${String.format(Locale.US, "%,.2f", bs.totalEquity)}", fontWeight = FontWeight.Bold, color = GoldPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun CashFlowTab(
    cf: com.example.obitrust.data.accounting.CashFlowStatement,
    currency: String
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                modifier = Modifier.fillMaxWidth().testTag("cash_flow_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Net Operating Cash Flow", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "$currency ${String.format(Locale.US, "%,.2f", cf.netCashFlow)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (cf.netCashFlow >= 0) EmeraldProfit else CrimsonExpense
                    )
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Cash Inflows", fontWeight = FontWeight.Bold, color = EmeraldProfit)
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Cash Receipts from Sales:", style = MaterialTheme.typography.bodySmall)
                        Text("+$currency ${String.format(Locale.US, "%,.2f", cf.cashInflowSales)}")
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Customer Debt Recoveries:", style = MaterialTheme.typography.bodySmall)
                        Text("+$currency ${String.format(Locale.US, "%,.2f", cf.cashInflowDebtCollections)}")
                    }
                    HorizontalDivider()
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Total Cash Inflows:", fontWeight = FontWeight.Bold)
                        Text("+$currency ${String.format(Locale.US, "%,.2f", cf.totalCashInflows)}", fontWeight = FontWeight.Bold, color = EmeraldProfit)
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Cash Outflows", fontWeight = FontWeight.Bold, color = CrimsonExpense)
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Stock Purchases Paid:", style = MaterialTheme.typography.bodySmall)
                        Text("-$currency ${String.format(Locale.US, "%,.2f", cf.cashOutflowInventoryPurchases)}")
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Operating Expenses Paid:", style = MaterialTheme.typography.bodySmall)
                        Text("-$currency ${String.format(Locale.US, "%,.2f", cf.cashOutflowOperatingExpenses)}")
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Supplier Debt Repayments:", style = MaterialTheme.typography.bodySmall)
                        Text("-$currency ${String.format(Locale.US, "%,.2f", cf.cashOutflowSupplierDebtPayments)}")
                    }
                    HorizontalDivider()
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Total Cash Outflows:", fontWeight = FontWeight.Bold)
                        Text("-$currency ${String.format(Locale.US, "%,.2f", cf.totalCashOutflows)}", fontWeight = FontWeight.Bold, color = CrimsonExpense)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartOfAccountsTab() {
    val accounts = listOf(
        Triple("1010", "Cash on Hand", "Asset • Normal Balance: Debit"),
        Triple("1020", "Bank Account (GCB/Ecobank)", "Asset • Normal Balance: Debit"),
        Triple("1030", "Mobile Money (MTN MoMo/Telecel)", "Asset • Normal Balance: Debit"),
        Triple("1100", "Accounts Receivable (Customer Debt)", "Asset • Normal Balance: Debit"),
        Triple("1200", "Merchandise Inventory", "Asset • Normal Balance: Debit"),
        Triple("2010", "Accounts Payable (Supplier Debt)", "Liability • Normal Balance: Credit"),
        Triple("2050", "Sales Tax Payable (GRA VAT)", "Liability • Normal Balance: Credit"),
        Triple("3010", "Owner's Capital", "Equity • Normal Balance: Credit"),
        Triple("3020", "Owner's Drawings", "Equity (Contra) • Normal Balance: Debit"),
        Triple("4010", "Sales Revenue", "Revenue • Normal Balance: Credit"),
        Triple("5010", "Cost of Goods Sold (COGS)", "Expense (Cost) • Normal Balance: Debit"),
        Triple("6010", "Rent Expense", "Expense • Normal Balance: Debit"),
        Triple("6020", "Electricity & Utilities", "Expense • Normal Balance: Debit"),
        Triple("6050", "Salaries & Wages", "Expense • Normal Balance: Debit"),
        Triple("6120", "Bank & MoMo Transfer Fees", "Expense • Normal Balance: Debit")
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("Standard Chart of Accounts", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("Double-entry mapping directory used by Obitrust Bookkeeping Engine", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        items(accounts) { (code, name, type) ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(GoldPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(code.take(2), fontWeight = FontWeight.Bold, color = GoldPrimary, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("$code - $name", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Text(type, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
