package com.example.obitrust.ui.suppliers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.obitrust.data.model.SupplierEntity
import com.example.obitrust.ui.BusinessViewModel
import com.example.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuppliersScreen(
    viewModel: BusinessViewModel,
    modifier: Modifier = Modifier
) {
    val suppliers by viewModel.suppliers.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val profile by viewModel.businessProfile.collectAsState()

    val currency = profile?.currencySymbol ?: "GH₵"

    var showAddDialog by remember { mutableStateOf(false) }
    var supplierToEdit by remember { mutableStateOf<SupplierEntity?>(null) }
    var supplierToPay by remember { mutableStateOf<SupplierEntity?>(null) }
    var payAmountText by remember { mutableStateOf("") }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: 1L) }

    val totalPayable = suppliers.sumOf { it.outstandingBalance }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    supplierToEdit = null
                    showAddDialog = true
                },
                containerColor = GoldPrimary,
                contentColor = NavyDark,
                modifier = Modifier.testTag("add_supplier_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Supplier")
            }
        },
        modifier = modifier.fillMaxSize().testTag("suppliers_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavyBorder)),
                modifier = Modifier.fillMaxWidth().testTag("supplier_summary_card")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total Accounts Payable (Supplier Debts)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "$currency ${String.format(Locale.US, "%,.2f", totalPayable)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFB923C)
                        )
                    }
                    Badge(containerColor = Color(0xFFFB923C).copy(alpha = 0.2f), contentColor = Color(0xFFFB923C)) {
                        Text("${suppliers.size} Suppliers", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text("Registered Suppliers & Manufacturers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(suppliers) { supplier ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth().testTag("supplier_card_${supplier.id}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(supplier.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "Contact: ${supplier.contactPerson} • ${supplier.phone}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (supplier.address.isNotEmpty()) {
                                        Text("Address: ${supplier.address}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                if (supplier.outstandingBalance > 0) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            "We Owe: $currency ${String.format(Locale.US, "%,.2f", supplier.outstandingBalance)}",
                                            fontWeight = FontWeight.Bold,
                                            color = CrimsonExpense
                                        )
                                        Text("Terms: ${supplier.paymentTermsDays} days", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                if (supplier.outstandingBalance > 0) {
                                    TextButton(
                                        onClick = {
                                            supplierToPay = supplier
                                            payAmountText = supplier.outstandingBalance.toString()
                                        }
                                    ) {
                                        Icon(Icons.Default.Payment, contentDescription = null, tint = EmeraldProfit, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Pay Supplier Balance", color = EmeraldProfit)
                                    }
                                }
                                TextButton(
                                    onClick = {
                                        supplierToEdit = supplier
                                        showAddDialog = true
                                    }
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

    // Add / Edit Supplier Dialog
    if (showAddDialog) {
        var name by remember { mutableStateOf(supplierToEdit?.name ?: "") }
        var contact by remember { mutableStateOf(supplierToEdit?.contactPerson ?: "") }
        var phone by remember { mutableStateOf(supplierToEdit?.phone ?: "") }
        var address by remember { mutableStateOf(supplierToEdit?.address ?: "") }
        var termsText by remember { mutableStateOf(supplierToEdit?.paymentTermsDays?.toString() ?: "30") }

        Dialog(onDismissRequest = { showAddDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(4.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(if (supplierToEdit == null) "Add Supplier" else "Edit Supplier", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Company / Supplier Name *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = contact, onValueChange = { contact = it }, label = { Text("Contact Person") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Warehouse / Factory Location") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = termsText, onValueChange = { termsText = it }, label = { Text("Payment Terms (Days)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showAddDialog = false }, modifier = Modifier.weight(1f)) { Text("Cancel") }
                        Button(
                            onClick = {
                                if (name.isNotBlank() && phone.isNotBlank()) {
                                    viewModel.saveSupplier(
                                        SupplierEntity(
                                            id = supplierToEdit?.id ?: 0,
                                            name = name.trim(),
                                            contactPerson = contact.trim(),
                                            phone = phone.trim(),
                                            address = address.trim(),
                                            outstandingBalance = supplierToEdit?.outstandingBalance ?: 0.0,
                                            paymentTermsDays = termsText.toIntOrNull() ?: 30
                                        )
                                    ) {
                                        showAddDialog = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save Supplier", color = NavyDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Pay Supplier Dialog
    supplierToPay?.let { supp ->
        Dialog(onDismissRequest = { supplierToPay = null }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(4.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Pay Supplier: ${supp.name}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Outstanding Balance: $currency ${String.format(Locale.US, "%.2f", supp.outstandingBalance)}", style = MaterialTheme.typography.bodySmall, color = CrimsonExpense)

                    OutlinedTextField(value = payAmountText, onValueChange = { payAmountText = it }, label = { Text("Payment Amount ($currency) *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                    Text("Paying Account:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                    LazyColumn(modifier = Modifier.heightIn(max = 120.dp)) {
                        items(accounts) { acc ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable { selectedAccountId = acc.id }
                            ) {
                                RadioButton(selected = selectedAccountId == acc.id, onClick = { selectedAccountId = acc.id })
                                Text("${acc.name} ($currency ${String.format(Locale.US, "%,.0f", acc.balance)})", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { supplierToPay = null }, modifier = Modifier.weight(1f)) { Text("Cancel") }
                        Button(
                            onClick = {
                                val amt = payAmountText.toDoubleOrNull() ?: 0.0
                                if (amt > 0) {
                                    viewModel.recordSupplierBalancePayment(supp.id, amt, selectedAccountId, "BANK_TRANSFER", "Settlement of supplier balance") {
                                        supplierToPay = null
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldProfit),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Disburse Payment", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
