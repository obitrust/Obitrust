package com.example.obitrust.data.accounting

import com.example.obitrust.data.model.JournalEntryEntity
import com.example.obitrust.data.model.JournalLineEntity

data class TrialBalanceRow(
    val accountCode: String,
    val accountName: String,
    val debitTotal: Double,
    val creditTotal: Double,
    val netDebit: Double,
    val netCredit: Double
)

data class TrialBalanceSummary(
    val rows: List<TrialBalanceRow>,
    val totalDebit: Double,
    val totalCredit: Double,
    val isBalanced: Boolean
)

data class ProfitLossStatement(
    val salesRevenue: Double,
    val costOfGoodsSold: Double,
    val grossProfit: Double,
    val grossMarginPercent: Double,
    val operatingExpenses: Map<String, Double>,
    val totalExpenses: Double,
    val netProfit: Double,
    val netMarginPercent: Double
)

data class BalanceSheet(
    val cashAndEquivalents: Double,
    val accountsReceivable: Double,
    val merchandiseInventory: Double,
    val totalCurrentAssets: Double,
    val totalAssets: Double,
    val accountsPayable: Double,
    val taxPayable: Double,
    val totalLiabilities: Double,
    val ownerCapital: Double,
    val ownerDrawings: Double,
    val currentPeriodNetProfit: Double,
    val totalEquity: Double,
    val totalLiabilitiesAndEquity: Double,
    val isBalanced: Boolean
)

data class CashFlowStatement(
    val cashInflowSales: Double,
    val cashInflowDebtCollections: Double,
    val totalCashInflows: Double,
    val cashOutflowInventoryPurchases: Double,
    val cashOutflowOperatingExpenses: Double,
    val cashOutflowSupplierDebtPayments: Double,
    val totalCashOutflows: Double,
    val netCashFlow: Double
)

object AccountingEngine {

    fun generateTrialBalance(lines: List<JournalLineEntity>): TrialBalanceSummary {
        val grouped = lines.groupBy { it.accountCode }
        val rows = mutableListOf<TrialBalanceRow>()
        var sumDebit = 0.0
        var sumCredit = 0.0

        for ((code, items) in grouped) {
            val name = items.firstOrNull()?.accountName ?: "Account $code"
            val deb = items.sumOf { it.debit }
            val cred = items.sumOf { it.credit }
            sumDebit += deb
            sumCredit += cred

            val netDebit = if (deb >= cred) deb - cred else 0.0
            val netCredit = if (cred > deb) cred - deb else 0.0

            rows.add(
                TrialBalanceRow(
                    accountCode = code,
                    accountName = name,
                    debitTotal = deb,
                    creditTotal = cred,
                    netDebit = netDebit,
                    netCredit = netCredit
                )
            )
        }

        rows.sortBy { it.accountCode }
        val isBalanced = kotlin.math.abs(sumDebit - sumCredit) < 0.01

        return TrialBalanceSummary(
            rows = rows,
            totalDebit = sumDebit,
            totalCredit = sumCredit,
            isBalanced = isBalanced
        )
    }

    fun generateProfitLoss(lines: List<JournalLineEntity>): ProfitLossStatement {
        val revenue = lines.filter { it.accountCode == ChartOfAccounts.CODE_SALES_REVENUE }.sumOf { it.credit - it.debit }.coerceAtLeast(0.0)
        val cogs = lines.filter { it.accountCode == ChartOfAccounts.CODE_COGS }.sumOf { it.debit - it.credit }.coerceAtLeast(0.0)
        val grossProfit = revenue - cogs
        val grossMargin = if (revenue > 0) (grossProfit / revenue) * 100 else 0.0

        val expenseLines = lines.filter { it.accountCode.startsWith("6") }
        val expensesByCategory = mutableMapOf<String, Double>()
        for (line in expenseLines) {
            val current = expensesByCategory.getOrDefault(line.accountName, 0.0)
            expensesByCategory[line.accountName] = current + (line.debit - line.credit)
        }

        val totalExpenses = expensesByCategory.values.sum()
        val netProfit = grossProfit - totalExpenses
        val netMargin = if (revenue > 0) (netProfit / revenue) * 100 else 0.0

        return ProfitLossStatement(
            salesRevenue = revenue,
            costOfGoodsSold = cogs,
            grossProfit = grossProfit,
            grossMarginPercent = grossMargin,
            operatingExpenses = expensesByCategory,
            totalExpenses = totalExpenses,
            netProfit = netProfit,
            netMarginPercent = netMargin
        )
    }

    fun generateBalanceSheet(
        lines: List<JournalLineEntity>,
        currentStockValuation: Double,
        actualCashBalances: Double,
        actualReceivables: Double,
        actualPayables: Double
    ): BalanceSheet {
        val pl = generateProfitLoss(lines)

        val cashAndEquivalents = actualCashBalances
        val ar = actualReceivables
        val inventory = currentStockValuation
        val totalAssets = cashAndEquivalents + ar + inventory

        val ap = actualPayables
        val taxPayable = lines.filter { it.accountCode == ChartOfAccounts.CODE_TAX_PAYABLE }.sumOf { it.credit - it.debit }.coerceAtLeast(0.0)
        val totalLiabilities = ap + taxPayable

        val capital = lines.filter { it.accountCode == ChartOfAccounts.CODE_CAPITAL }.sumOf { it.credit - it.debit }.coerceAtLeast(0.0).let {
            if (it == 0.0) (totalAssets - totalLiabilities - pl.netProfit).coerceAtLeast(5000.0) else it
        }
        val drawings = lines.filter { it.accountCode == ChartOfAccounts.CODE_DRAWINGS }.sumOf { it.debit - it.credit }.coerceAtLeast(0.0)
        val totalEquity = capital - drawings + pl.netProfit
        val totalLiabAndEquity = totalLiabilities + totalEquity
        val isBalanced = kotlin.math.abs(totalAssets - totalLiabAndEquity) < 1.0

        return BalanceSheet(
            cashAndEquivalents = cashAndEquivalents,
            accountsReceivable = ar,
            merchandiseInventory = inventory,
            totalCurrentAssets = totalAssets,
            totalAssets = totalAssets,
            accountsPayable = ap,
            taxPayable = taxPayable,
            totalLiabilities = totalLiabilities,
            ownerCapital = capital,
            ownerDrawings = drawings,
            currentPeriodNetProfit = pl.netProfit,
            totalEquity = totalEquity,
            totalLiabilitiesAndEquity = totalLiabAndEquity,
            isBalanced = isBalanced
        )
    }

    fun generateCashFlow(
        inflowSales: Double,
        inflowDebts: Double,
        outflowPurchases: Double,
        outflowExpenses: Double,
        outflowSuppliers: Double
    ): CashFlowStatement {
        val totalIn = inflowSales + inflowDebts
        val totalOut = outflowPurchases + outflowExpenses + outflowSuppliers
        return CashFlowStatement(
            cashInflowSales = inflowSales,
            cashInflowDebtCollections = inflowDebts,
            totalCashInflows = totalIn,
            cashOutflowInventoryPurchases = outflowPurchases,
            cashOutflowOperatingExpenses = outflowExpenses,
            cashOutflowSupplierDebtPayments = outflowSuppliers,
            totalCashOutflows = totalOut,
            netCashFlow = totalIn - totalOut
        )
    }
}
