package com.example.obitrust.ui.expenses

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.obitrust.data.model.ExpenseEntity
import com.example.obitrust.ui.BusinessViewModel
import com.example.obitrust.ui.components.SimpleDonutChart
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    viewModel: BusinessViewModel,
    modifier: Modifier = Modifier
) {
    val expenses by viewModel.expenses.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val profile by viewModel.businessProfile.collectAsState()

    val currency = profile?.currencySymbol ?: "GH₵"
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedCategoryFilter by remember { mutableStateOf("All") }

    val categories = listOf(
        "All", "Rent", "Electricity", "Water", "Transportation", "Salaries",
        "Fuel", "Internet", "Maintenance", "Food", "Stationery", "Marketing", "Miscellaneous"
    )

    val filteredExpenses = remember(expenses, selectedCategoryFilter) {
        if (selectedCategoryFilter == "All") expenses else expenses.filter { it.category == selectedCategoryFilter }
    }

    val totalExpense = expenses.sumOf { it.amount }
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    val categoryBreakdown = remember(expenses) {
        expenses.groupBy { it.category }
            .map { (cat, list) -> cat to list.sumOf { it.amount } }
            .sortedByDescending { it.second }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = CrimsonExpense,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_expense_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Log Expense")
            }
        },
        modifier = modifier.fillMaxSize().testTag("expenses_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Expenditure Card
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = NavySurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavyBorder)),
                    modifier = Modifier.fillMaxWidth().testTag("total_expenses_card")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Operating Expenses", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "$currency ${String.format(Locale.US, "%,.2f", totalExpense)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = CrimsonExpense
                            )
                        }
                        Badge(containerColor = CrimsonExpense.copy(alpha = 0.2f), contentColor = CrimsonExpense) {
                            Text("${expenses.size} Outlays", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Category Donut Chart
            if (categoryBreakdown.isNotEmpty()) {
                item {
                    Text("Expense Breakdown by Category", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    SimpleDonutChart(categories = categoryBreakdown)
                }
            }

            // Filter Chips
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategoryFilter == cat,
                            onClick = { selectedCategoryFilter = cat },
                            label = { Text(cat) }
                        )
                    }
                }
            }

            // Expense Entries List
            if (filteredExpenses.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                        Text("No expenses logged in this category.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(filteredExpenses) { exp ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth().testTag("expense_card_${exp.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(exp.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${exp.category} • Paid via ${exp.paymentMethod}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(sdf.format(Date(exp.createdAt)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                "-$currency ${String.format(Locale.US, "%,.2f", exp.amount)}",
                                fontWeight = FontWeight.Bold,
                                color = CrimsonExpense,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        RecordExpenseDialog(
            accounts = accounts,
            currency = currency,
            onDismiss = { showAddDialog = false },
            onConfirm = { title, cat, amt, accId, method, isRec, notes ->
                viewModel.recordExpense(title, cat, amt, accId, method, isRec, notes) {
                    showAddDialog = false
                }
            }
        )
    }
}

@Composable
private fun RecordExpenseDialog(
    accounts: List<com.example.obitrust.data.model.FinancialAccountEntity>,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double, Long?, String, Boolean, String) -> Unit
) {
    val categories = listOf(
        "Rent", "Electricity", "Water", "Transportation", "Salaries",
        "Fuel", "Internet", "Maintenance", "Food", "Stationery", "Marketing", "Miscellaneous"
    )

    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Electricity") }
    var amountText by remember { mutableStateOf("") }
    var selectedPaymentMethod by remember { mutableStateOf("CASH") }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id) }
    var isRecurring by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(4.dp).testTag("record_expense_dialog")
        ) {
            LazyColumn(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Text("Log Business Expense", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Deducts cash/MoMo and creates general ledger debit entry", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Expense Title / Memo *") },
                        modifier = Modifier.fillMaxWidth().testTag("expense_title_input"),
                        singleLine = true
                    )
                }

                item {
                    Text("Category:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat) }
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount ($currency) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("expense_amount_input"),
                        singleLine = true
                    )
                }

                item {
                    Text("Payment Channel:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("CASH" to "Cash", "MTN_MOMO" to "MoMo", "BANK_TRANSFER" to "Bank").forEach { (code, label) ->
                            FilterChip(
                                selected = selectedPaymentMethod == code,
                                onClick = { selectedPaymentMethod = code },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                item {
                    Text("Paying Account:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(accounts) { acc ->
                            FilterChip(
                                selected = selectedAccountId == acc.id,
                                onClick = { selectedAccountId = acc.id },
                                label = { Text("${acc.name} ($currency ${String.format(Locale.US, "%,.0f", acc.balance)})") }
                            )
                        }
                    }
                }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isRecurring, onCheckedChange = { isRecurring = it })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Set as recurring monthly expense (e.g. Rent, Internet)")
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                        Button(
                            onClick = {
                                val amt = amountText.toDoubleOrNull() ?: 0.0
                                if (title.isNotBlank() && amt > 0) {
                                    onConfirm(title, selectedCategory, amt, selectedAccountId, selectedPaymentMethod, isRecurring, notes)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonExpense),
                            modifier = Modifier.weight(1f).testTag("save_expense_button")
                        ) {
                            Text("Post Expense", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
