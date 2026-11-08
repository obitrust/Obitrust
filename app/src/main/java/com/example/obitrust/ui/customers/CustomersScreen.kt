package com.example.obitrust.ui.customers

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.obitrust.data.model.CustomerEntity
import com.example.obitrust.ui.BusinessViewModel
import com.example.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    viewModel: BusinessViewModel,
    modifier: Modifier = Modifier
) {
    val customers by viewModel.customers.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val profile by viewModel.businessProfile.collectAsState()
    val context = LocalContext.current

    val currency = profile?.currencySymbol ?: "GH₵"

    var showAddDialog by remember { mutableStateOf(false) }
    var customerToEdit by remember { mutableStateOf<CustomerEntity?>(null) }
    var customerToPayDebt by remember { mutableStateOf<CustomerEntity?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val totalDebt = customers.sumOf { it.outstandingDebt }
    val debtorsCount = customers.filter { it.outstandingDebt > 0 }.size

    val filteredCustomers = remember(customers, searchQuery) {
        customers.filter {
            searchQuery.isEmpty() ||
                    it.name.contains(searchQuery, ignoreCase = true) ||
                    it.phone.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    customerToEdit = null
                    showAddDialog = true
                },
                containerColor = GoldPrimary,
                contentColor = NavyDark,
                modifier = Modifier.testTag("add_customer_fab")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add Customer")
            }
        },
        modifier = modifier.fillMaxSize().testTag("customers_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Debt Summary Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavyBorder)),
                modifier = Modifier.fillMaxWidth().testTag("customer_debt_summary")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Outstanding Customer Debts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "$currency ${String.format(Locale.US, "%,.2f", totalDebt)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = CrimsonExpense
                        )
                    }
                    Badge(containerColor = CrimsonExpense.copy(alpha = 0.2f), contentColor = CrimsonExpense) {
                        Text("$debtorsCount Debtors", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search customer by name or phone...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("customer_search_field")
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredCustomers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No customers found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredCustomers) { customer ->
                        val hasDebt = customer.outstandingDebt > 0
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth().testTag("customer_card_${customer.id}")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(customer.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            "Tel: ${customer.phone} ${if (customer.address.isNotEmpty()) "• ${customer.address}" else ""}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (hasDebt) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                "Owes: $currency ${String.format(Locale.US, "%,.2f", customer.outstandingDebt)}",
                                                fontWeight = FontWeight.Bold,
                                                color = CrimsonExpense
                                            )
                                            Text("Credit Limit: $currency ${String.format(Locale.US, "%,.0f", customer.creditLimit)}", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (hasDebt) {
                                        TextButton(
                                            onClick = {
                                                val msg = "Dear ${customer.name}, friendly reminder from ${profile?.businessName ?: "OBITRUST"}. Your outstanding balance is $currency ${String.format(Locale.US, "%.2f", customer.outstandingDebt)}. Kindly settle via MTN MoMo / Telecel Cash or bank. Thank you!"
                                                val sendIntent = Intent().apply {
                                                    action = Intent.ACTION_SEND
                                                    putExtra(Intent.EXTRA_TEXT, msg)
                                                    type = "text/plain"
                                                }
                                                context.startActivity(Intent.createChooser(sendIntent, "Send WhatsApp Debt Reminder"))
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = null, tint = EmeraldProfit, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("WhatsApp Reminder", color = EmeraldProfit)
                                        }

                                        TextButton(
                                            onClick = { customerToPayDebt = customer },
                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                        ) {
                                            Icon(Icons.Default.Payment, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Receive Payment", color = GoldPrimary)
                                        }
                                    }

                                    TextButton(
                                        onClick = {
                                            customerToEdit = customer
                                            showAddDialog = true
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Text("Edit")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Customer Dialog
    if (showAddDialog) {
        AddEditCustomerDialog(
            customer = customerToEdit,
            currency = currency,
            onDismiss = { showAddDialog = false },
            onSave = { c ->
                viewModel.saveCustomer(c) {
                    showAddDialog = false
                }
            }
        )
    }

    // Receive Debt Payment Dialog
    customerToPayDebt?.let { cust ->
        ReceiveDebtPaymentDialog(
            customer = cust,
            accounts = accounts,
            currency = currency,
            onDismiss = { customerToPayDebt = null },
            onConfirm = { amount, accId, method, notes ->
                viewModel.recordCustomerDebtPayment(cust.id, amount, accId, method, notes) {
                    customerToPayDebt = null
                }
            }
        )
    }
}

@Composable
private fun AddEditCustomerDialog(
    customer: CustomerEntity?,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (CustomerEntity) -> Unit
) {
    var name by remember { mutableStateOf(customer?.name ?: "") }
    var phone by remember { mutableStateOf(customer?.phone ?: "") }
    var email by remember { mutableStateOf(customer?.email ?: "") }
    var address by remember { mutableStateOf(customer?.address ?: "") }
    var creditLimitText by remember { mutableStateOf(customer?.creditLimit?.toString() ?: "5000") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(4.dp).testTag("customer_form_dialog")
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (customer == null) "Register New Customer" else "Edit Customer", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Customer Name *") },
                    modifier = Modifier.fillMaxWidth().testTag("customer_name_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number (+233) *") },
                    modifier = Modifier.fillMaxWidth().testTag("customer_phone_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Location / Business Address") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = creditLimitText,
                    onValueChange = { creditLimitText = it },
                    label = { Text("Credit Limit ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(
                        onClick = {
                            if (name.isNotBlank() && phone.isNotBlank()) {
                                onSave(
                                    CustomerEntity(
                                        id = customer?.id ?: 0,
                                        name = name.trim(),
                                        phone = phone.trim(),
                                        email = email.trim(),
                                        address = address.trim(),
                                        outstandingDebt = customer?.outstandingDebt ?: 0.0,
                                        creditLimit = creditLimitText.toDoubleOrNull() ?: 5000.0
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                        modifier = Modifier.weight(1f).testTag("save_customer_button")
                    ) {
                        Text("Save Customer", color = NavyDark, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiveDebtPaymentDialog(
    customer: CustomerEntity,
    accounts: List<com.example.obitrust.data.model.FinancialAccountEntity>,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (Double, Long, String, String) -> Unit
) {
    var amountText by remember { mutableStateOf(customer.outstandingDebt.toString()) }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: 1L) }
    var paymentMethod by remember { mutableStateOf("MTN_MOMO") }
    var notes by remember { mutableStateOf("Debt settlement payment") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(4.dp).testTag("receive_debt_dialog")
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Receive Debt Payment", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Customer: ${customer.name} (Owes: $currency ${String.format(Locale.US, "%.2f", customer.outstandingDebt)})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount Received ($currency) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("payment_amount_input"),
                    singleLine = true
                )

                Text("Deposit Account:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    accounts.forEach { acc ->
                        FilterChip(
                            selected = selectedAccountId == acc.id,
                            onClick = { selectedAccountId = acc.id },
                            label = { Text(acc.name) }
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (amt > 0) {
                                onConfirm(amt, selectedAccountId, paymentMethod, notes)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldProfit),
                        modifier = Modifier.weight(1f).testTag("confirm_debt_payment")
                    ) {
                        Text("Record Payment", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
