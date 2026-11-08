package com.example.obitrust.ui.ai

import com.example.obitrust.data.accounting.AccountingEngine
import com.example.obitrust.data.model.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class AiChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "USER" or "AI"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val metricsBadge: String? = null
)

object AiAccountantEngine {

    val quickQuestions = listOf(
        "How much did I sell today?",
        "What is my profit this month?",
        "Which products are selling fastest?",
        "Which products are running out?",
        "How much do customers owe me?",
        "How much do I owe suppliers?",
        "What are my biggest expenses?",
        "Which products have highest profit margins?",
        "How much stock value do I currently have?",
        "Is my business making a profit or loss?",
        "Give me a financial summary for this month."
    )

    fun analyzeAndAnswer(
        question: String,
        sales: List<SaleEntity>,
        saleItems: List<SaleItemEntity>,
        products: List<ProductEntity>,
        expenses: List<ExpenseEntity>,
        customers: List<CustomerEntity>,
        suppliers: List<SupplierEntity>,
        accounts: List<FinancialAccountEntity>,
        journalLines: List<JournalLineEntity>,
        currency: String = "GH₵"
    ): AiChatMessage {
        val q = question.lowercase(Locale.ROOT).trim()

        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, 1)
        val monthStart = cal.timeInMillis

        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        val weekStart = cal.timeInMillis

        // Calculations
        val validSales = sales.filter { it.status != "VOIDED" }
        val salesToday = validSales.filter { it.createdAt >= todayStart }
        val salesThisMonth = validSales.filter { it.createdAt >= monthStart }
        val salesThisWeek = validSales.filter { it.createdAt >= weekStart }

        val todayRevenue = salesToday.sumOf { it.totalAmount }
        val monthRevenue = salesThisMonth.sumOf { it.totalAmount }
        val weekRevenue = salesThisWeek.sumOf { it.totalAmount }

        val expensesThisMonth = expenses.filter { it.createdAt >= monthStart }
        val monthExpenseTotal = expensesThisMonth.sumOf { it.amount }

        val totalStockValueCost = products.sumOf { it.costPrice * it.quantity }
        val totalStockValueRetail = products.sumOf { it.sellingPrice * it.quantity }

        val totalCustomerDebt = customers.sumOf { it.outstandingDebt }
        val totalSupplierDebt = suppliers.sumOf { it.outstandingBalance }

        val totalCashBalance = accounts.sumOf { it.balance }

        val pl = AccountingEngine.generateProfitLoss(journalLines)

        return when {
            q.contains("sell today") || q.contains("sales today") || (q.contains("today") && q.contains("sale")) -> {
                val count = salesToday.size
                val cashPaid = salesToday.sumOf { it.amountPaid }
                val creditGiven = salesToday.sumOf { it.balanceDue }
                val badge = "$currency ${formatMoney(todayRevenue)}"
                val msg = buildString {
                    appendLine("📊 **Today's Sales Performance:**")
                    appendLine("• **Total Sales:** $currency ${formatMoney(todayRevenue)} ($count completed transactions)")
                    appendLine("• **Cash/MoMo Collected:** $currency ${formatMoney(cashPaid)}")
                    if (creditGiven > 0) {
                        appendLine("• **Credit Extended:** $currency ${formatMoney(creditGiven)}")
                    }
                    appendLine("\n💡 *Accountant's Advice:* ")
                    if (todayRevenue > 0) {
                        append("Ensure all physical cash in the drawer matches your registered sales before closing today's books.")
                    } else {
                        append("No sales recorded yet today. Check inventory stock levels and ensure your store POS is active.")
                    }
                }
                AiChatMessage(sender = "AI", text = msg, metricsBadge = badge)
            }

            q.contains("profit this month") || (q.contains("profit") && q.contains("month")) || q.contains("net profit") -> {
                val gross = pl.grossProfit
                val net = pl.netProfit
                val badge = if (net >= 0) "Net: $currency ${formatMoney(net)}" else "Loss: $currency ${formatMoney(net)}"
                val msg = buildString {
                    appendLine("📈 **Profit & Loss Analysis (This Period):**")
                    appendLine("• **Total Sales Revenue:** $currency ${formatMoney(pl.salesRevenue)}")
                    appendLine("• **Cost of Goods Sold (COGS):** $currency ${formatMoney(pl.costOfGoodsSold)}")
                    appendLine("• **Gross Profit:** $currency ${formatMoney(gross)} (${formatPercent(pl.grossMarginPercent)} margin)")
                    appendLine("• **Operating Expenses:** $currency ${formatMoney(pl.totalExpenses)}")
                    appendLine("• **Net Profit:** $currency ${formatMoney(net)} (${formatPercent(pl.netMarginPercent)} net margin)")
                    appendLine("\n💡 *Accountant's Assessment:* ")
                    if (net > 0) {
                        append("Your business is operating at a solid net profit margin of ${formatPercent(pl.netMarginPercent)}. Keep fixed overheads controlled to maximize retained earnings.")
                    } else if (net == 0.0 && pl.salesRevenue == 0.0) {
                        append("No sales recorded for this period yet to determine net profit.")
                    } else {
                        append("Your current expenses exceed your gross margins. Review top expenditure items like Rent, Salaries, or Utility units to restore healthy operating margins.")
                    }
                }
                AiChatMessage(sender = "AI", text = msg, metricsBadge = badge)
            }

            q.contains("fastest") || q.contains("best selling") || q.contains("top product") -> {
                val grouped = saleItems.groupBy { it.productId }
                val itemVolumes = grouped.map { (pid, items) ->
                    val name = items.firstOrNull()?.productName ?: "Product $pid"
                    val qty = items.sumOf { it.quantity }
                    val revenue = items.sumOf { it.subtotal }
                    Triple(name, qty, revenue)
                }.sortedByDescending { it.second }

                val top5 = itemVolumes.take(5)
                val badge = if (top5.isNotEmpty()) "Top: ${top5.first().first}" else null
                val msg = buildString {
                    appendLine("🔥 **Best-Selling Products by Sales Volume:**")
                    if (top5.isEmpty()) {
                        appendLine("No sales items recorded yet.")
                    } else {
                        top5.forEachIndexed { idx, item ->
                            appendLine("${idx + 1}. **${item.first}** — ${item.second} units sold ($currency ${formatMoney(item.third)})")
                        }
                    }
                    appendLine("\n💡 *Inventory Advice:* ")
                    append("Maintain at least 2 weeks of safety stock for your top 3 fast movers to prevent costly stockouts during peak shopping hours.")
                }
                AiChatMessage(sender = "AI", text = msg, metricsBadge = badge)
            }

            q.contains("running out") || q.contains("low stock") || q.contains("out of stock") -> {
                val outOfStock = products.filter { it.quantity <= 0 }
                val lowStock = products.filter { it.quantity > 0 && it.quantity <= it.minStockLevel }
                val badge = "${outOfStock.size} Out | ${lowStock.size} Low"
                val msg = buildString {
                    appendLine("⚠️ **Inventory Stock Alert:**")
                    if (outOfStock.isNotEmpty()) {
                        appendLine("\n🚨 **Out of Stock (${outOfStock.size} items):**")
                        outOfStock.take(5).forEach {
                            appendLine("• **${it.name}** (Min: ${it.minStockLevel}) — Reorder immediately from ${it.supplierName.ifEmpty { "Supplier" }}")
                        }
                    }
                    if (lowStock.isNotEmpty()) {
                        appendLine("\n🟡 **Low Stock (${lowStock.size} items):**")
                        lowStock.take(5).forEach {
                            appendLine("• **${it.name}** — ${it.quantity} units left (Min threshold: ${it.minStockLevel})")
                        }
                    }
                    if (outOfStock.isEmpty() && lowStock.isEmpty()) {
                        appendLine("✅ All products are currently above their minimum safety thresholds. Great job maintaining optimal inventory levels!")
                    } else {
                        appendLine("\n💡 *Procurement Recommendation:* Generate a Purchase Order for your depleted items before supplier delivery lead times impact your weekend sales.")
                    }
                }
                AiChatMessage(sender = "AI", text = msg, metricsBadge = badge)
            }

            q.contains("customers owe") || q.contains("debtors") || q.contains("receivable") -> {
                val debtors = customers.filter { it.outstandingDebt > 0 }.sortedByDescending { it.outstandingDebt }
                val badge = "$currency ${formatMoney(totalCustomerDebt)}"
                val msg = buildString {
                    appendLine("👥 **Customer Receivables (Debts Owed to Business):**")
                    appendLine("• **Total Outstanding Customer Debt:** $currency ${formatMoney(totalCustomerDebt)}")
                    appendLine("• **Total Debtors:** ${debtors.size} customers")
                    if (debtors.isNotEmpty()) {
                        appendLine("\n**Top Debtors:**")
                        debtors.take(5).forEachIndexed { idx, c ->
                            appendLine("${idx + 1}. **${c.name}** (${c.phone}) — $currency ${formatMoney(c.outstandingDebt)}")
                        }
                    }
                    appendLine("\n💡 *Cash Collection Advice:* ")
                    append("Use the Obitrust WhatsApp debt reminder feature to send courtesy balance statements to debtors over 14 days overdue.")
                }
                AiChatMessage(sender = "AI", text = msg, metricsBadge = badge)
            }

            q.contains("owe suppliers") || q.contains("supplier debt") || q.contains("payable") || q.contains("creditor") -> {
                val creditors = suppliers.filter { it.outstandingBalance > 0 }.sortedByDescending { it.outstandingBalance }
                val badge = "$currency ${formatMoney(totalSupplierDebt)}"
                val msg = buildString {
                    appendLine("🚚 **Supplier Payables (Accounts Payable):**")
                    appendLine("• **Total Owed to Suppliers:** $currency ${formatMoney(totalSupplierDebt)}")
                    appendLine("• **Creditor Accounts:** ${creditors.size} suppliers")
                    if (creditors.isNotEmpty()) {
                        appendLine("\n**Top Supplier Balances:**")
                        creditors.take(5).forEachIndexed { idx, s ->
                            appendLine("${idx + 1}. **${s.name}** — $currency ${formatMoney(s.outstandingBalance)} (Terms: ${s.paymentTermsDays} days)")
                        }
                    }
                    appendLine("\n💡 *Credit Standing Advice:* ")
                    append("Settling supplier balances within agreed payment terms preserves supplier discounts and ensures priority restocking during supply shortages.")
                }
                AiChatMessage(sender = "AI", text = msg, metricsBadge = badge)
            }

            q.contains("biggest expense") || q.contains("expenses") || q.contains("spending") -> {
                val grouped = expenses.groupBy { it.category }.mapValues { (_, list) -> list.sumOf { it.amount } }
                val sorted = grouped.toList().sortedByDescending { it.second }
                val badge = "$currency ${formatMoney(expenses.sumOf { it.amount })}"
                val msg = buildString {
                    appendLine("💸 **Expense Breakdown & Expenditure:**")
                    appendLine("• **Total Expenses Logged:** $currency ${formatMoney(expenses.sumOf { it.amount })}")
                    if (sorted.isNotEmpty()) {
                        appendLine("\n**Top Expense Categories:**")
                        sorted.take(6).forEachIndexed { idx, (cat, amt) ->
                            val pct = if (expenses.sumOf { it.amount } > 0) (amt / expenses.sumOf { it.amount }) * 100 else 0.0
                            appendLine("${idx + 1}. **$cat:** $currency ${formatMoney(amt)} (${formatPercent(pct)})")
                        }
                    }
                    appendLine("\n💡 *Cost Optimization Advice:* ")
                    append("Target your top 2 expense drivers for negotiated monthly bulk rates or energy efficiency savings to boost net profitability.")
                }
                AiChatMessage(sender = "AI", text = msg, metricsBadge = badge)
            }

            q.contains("highest profit") || q.contains("highest margin") || q.contains("best margin") -> {
                val margins = products.map { p ->
                    val margin = if (p.sellingPrice > 0) ((p.sellingPrice - p.costPrice) / p.sellingPrice) * 100 else 0.0
                    val unitProfit = p.sellingPrice - p.costPrice
                    p to (margin to unitProfit)
                }.sortedByDescending { it.second.first }

                val top5 = margins.take(5)
                val badge = if (top5.isNotEmpty()) "${formatPercent(top5.first().second.first)} Margin" else null
                val msg = buildString {
                    appendLine("💎 **Highest Margin Products:**")
                    top5.forEachIndexed { idx, (p, info) ->
                        appendLine("${idx + 1}. **${p.name}** — Margin: **${formatPercent(info.first)}** (Profit: $currency ${formatMoney(info.second)} per unit)")
                    }
                    appendLine("\n💡 *Merchandising Strategy:* ")
                    append("Position these high-margin items at eye level in your store or bundle them with fast-moving staples to elevate basket size.")
                }
                AiChatMessage(sender = "AI", text = msg, metricsBadge = badge)
            }

            q.contains("stock value") || q.contains("inventory value") || q.contains("worth") -> {
                val count = products.sumOf { it.quantity }
                val badge = "$currency ${formatMoney(totalStockValueCost)}"
                val msg = buildString {
                    appendLine("📦 **Current Stock Valuation:**")
                    appendLine("• **Stock Value at Cost Price:** $currency ${formatMoney(totalStockValueCost)}")
                    appendLine("• **Potential Retail Value:** $currency ${formatMoney(totalStockValueRetail)}")
                    appendLine("• **Projected Gross Inventory Profit:** $currency ${formatMoney(totalStockValueRetail - totalStockValueCost)}")
                    appendLine("• **Total Units in Store:** $count items across ${products.size} product lines")
                    appendLine("\n💡 *Audit Control:* Perform a weekly cycle count on high-value items to ensure ledger values match actual warehouse inventory.")
                }
                AiChatMessage(sender = "AI", text = msg, metricsBadge = badge)
            }

            q.contains("profit or loss") || q.contains("health") || q.contains("financial summary") || q.contains("summary") -> {
                val net = pl.netProfit
                val badge = if (net >= 0) "Profitable: $currency ${formatMoney(net)}" else "Operating Deficit"
                val msg = buildString {
                    appendLine("📊 **Comprehensive Business Health Diagnostic:**")
                    appendLine("• **Total Revenue:** $currency ${formatMoney(pl.salesRevenue)}")
                    appendLine("• **Gross Profit:** $currency ${formatMoney(pl.grossProfit)} (${formatPercent(pl.grossMarginPercent)})")
                    appendLine("• **Operating Expenses:** $currency ${formatMoney(pl.totalExpenses)}")
                    appendLine("• **Net Profit:** $currency ${formatMoney(net)} (${formatPercent(pl.netMarginPercent)})")
                    appendLine("• **Available Cash & Bank/MoMo:** $currency ${formatMoney(totalCashBalance)}")
                    appendLine("• **Customer Receivables:** $currency ${formatMoney(totalCustomerDebt)}")
                    appendLine("• **Supplier Payables:** $currency ${formatMoney(totalSupplierDebt)}")
                    appendLine("• **Merchandise Stock:** $currency ${formatMoney(totalStockValueCost)}")
                    appendLine("\n🎯 **Executive Verdict:**")
                    if (net > 0 && totalCashBalance > totalSupplierDebt) {
                        append("Your enterprise is financially sound, solvent, and generating positive net return. Working capital is healthy and receivables are adequately covered.")
                    } else if (net > 0) {
                        append("Your sales are profitable, but your cash is tied up in customer debts or heavy stock inventory. Accelerate debt collections to bolster liquid reserves.")
                    } else {
                        append("The business is currently operating at an operational deficit. Focus on moving slow stock and reducing non-essential overheads immediately.")
                    }
                }
                AiChatMessage(sender = "AI", text = msg, metricsBadge = badge)
            }

            else -> {
                // General assistant response with dynamic real business snapshot
                val msg = buildString {
                    appendLine("👋 **Hello! I am OBITRUST AI ACCOUNTANT.**")
                    appendLine("I am continuously auditing your live database. Here is your current business pulse:")
                    appendLine("• **Today's Sales:** $currency ${formatMoney(todayRevenue)}")
                    appendLine("• **Month Sales:** $currency ${formatMoney(monthRevenue)}")
                    appendLine("• **Liquid Cash & MoMo:** $currency ${formatMoney(totalCashBalance)}")
                    appendLine("• **Stock Valuation:** $currency ${formatMoney(totalStockValueCost)}")
                    appendLine("• **Customer Debt:** $currency ${formatMoney(totalCustomerDebt)}")
                    appendLine("\nYou can ask me specific questions like:")
                    appendLine("• *'How much did I sell today?'*")
                    appendLine("• *'What is my profit this month?'*")
                    appendLine("• *'Which products are running out?'*")
                    appendLine("• *'Who owes the business?'*")
                    appendLine("• *'What are my biggest expenses?'*")
                }
                AiChatMessage(sender = "AI", text = msg)
            }
        }
    }

    private fun formatMoney(amount: Double): String {
        return String.format(Locale.US, "%,.2f", amount)
    }

    private fun formatPercent(value: Double): String {
        return String.format(Locale.US, "%.1f%%", value)
    }
}
