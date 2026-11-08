package com.example.obitrust.ui.purchases

import androidx.compose.foundation.background
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
import com.example.obitrust.data.model.ProductEntity
import com.example.obitrust.data.model.PurchaseEntity
import com.example.obitrust.data.model.SupplierEntity
import com.example.obitrust.ui.BusinessViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchasesScreen(
    viewModel: BusinessViewModel,
    modifier: Modifier = Modifier
) {
    val purchases by viewModel.purchases.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val products by viewModel.products.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val profile by viewModel.businessProfile.collectAsState()

    val currency = profile?.currencySymbol ?: "GH₵"
    var showNewPurchaseDialog by remember { mutableStateOf(false) }

    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewPurchaseDialog = true },
                containerColor = GoldPrimary,
                contentColor = NavyDark,
                modifier = Modifier.testTag("record_purchase_fab")
            ) {
                Icon(Icons.Default.AddBusiness, contentDescription = "Record Stock Purchase")
            }
        },
        modifier = modifier.fillMaxSize().testTag("purchases_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Header Stats
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavyBorder)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total Stock Purchases", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "$currency ${String.format(Locale.US, "%,.2f", purchases.sumOf { it.totalAmount })}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary
                        )
                    }
                    Badge(containerColor = GoldPrimary.copy(alpha = 0.2f), contentColor = GoldPrimary) {
                        Text("${purchases.size} Invoices", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text("Stock Purchase History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            if (purchases.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No stock purchases recorded yet. Tap + to restock.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(purchases) { purchase ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth().testTag("purchase_card_${purchase.id}")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("#${purchase.purchaseNumber}", fontWeight = FontWeight.Bold)
                                        Text("Supplier: ${purchase.supplierName}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            "$currency ${String.format(Locale.US, "%,.2f", purchase.totalAmount)}",
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldProfit
                                        )
                                        Text(sdf.format(Date(purchase.createdAt)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                if (purchase.balanceDue > 0) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "Owed to Supplier: $currency ${String.format(Locale.US, "%.2f", purchase.balanceDue)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CrimsonExpense,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewPurchaseDialog) {
        RecordPurchaseDialog(
            suppliers = suppliers,
            products = products,
            accounts = accounts,
            currency = currency,
            onDismiss = { showNewPurchaseDialog = false },
            onConfirm = { supp, suppName, invoice, items, paid, method, accId, notes ->
                viewModel.recordPurchase(
                    supplier = supp,
                    supplierName = suppName,
                    invoiceNumber = invoice,
                    items = items,
                    amountPaid = paid,
                    paymentMethod = method,
                    paymentAccountId = accId,
                    notes = notes,
                    onSuccess = { showNewPurchaseDialog = false }
                )
            }
        )
    }
}

@Composable
private fun RecordPurchaseDialog(
    suppliers: List<SupplierEntity>,
    products: List<ProductEntity>,
    accounts: List<com.example.obitrust.data.model.FinancialAccountEntity>,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (SupplierEntity?, String, String, List<Pair<ProductEntity, Pair<Int, Double>>>, Double, String, Long?, String) -> Unit
) {
    var selectedSupplier by remember { mutableStateOf<SupplierEntity?>(suppliers.firstOrNull()) }
    var customSupplierName by remember { mutableStateOf("") }
    var invoiceNumber by remember { mutableStateOf("") }
    var selectedProduct by remember { mutableStateOf<ProductEntity?>(products.firstOrNull()) }
    var purchaseQtyText by remember { mutableStateOf("10") }
    var unitCostText by remember { mutableStateOf(products.firstOrNull()?.costPrice?.toString() ?: "0") }
    var amountPaidText by remember { mutableStateOf("") }
    var selectedPaymentMethod by remember { mutableStateOf("CASH") }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id) }
    var notes by remember { mutableStateOf("") }

    val qty = purchaseQtyText.toIntOrNull() ?: 1
    val cost = unitCostText.toDoubleOrNull() ?: 0.0
    val totalPurchaseCost = qty * cost

    LaunchedEffect(totalPurchaseCost) {
        if (amountPaidText.isEmpty()) {
            amountPaidText = String.format(Locale.US, "%.2f", totalPurchaseCost)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(4.dp).testTag("record_purchase_dialog")
        ) {
            LazyColumn(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text("Record Stock Purchase", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Incoming stock will be credited directly to inventory", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                item {
                    Text("Select Product to Restock:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(products) { p ->
                            FilterChip(
                                selected = selectedProduct?.id == p.id,
                                onClick = {
                                    selectedProduct = p
                                    unitCostText = p.costPrice.toString()
                                },
                                label = { Text(p.name) }
                            )
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = purchaseQtyText,
                            onValueChange = { purchaseQtyText = it },
                            label = { Text("Quantity Restocked") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = unitCostText,
                            onValueChange = { unitCostText = it },
                            label = { Text("Unit Cost ($currency)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                item {
                    Text("Select Supplier:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(suppliers) { s ->
                            FilterChip(
                                selected = selectedSupplier?.id == s.id,
                                onClick = { selectedSupplier = s },
                                label = { Text(s.name) }
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = invoiceNumber,
                        onValueChange = { invoiceNumber = it },
                        label = { Text("Supplier Invoice / Waybill #") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = amountPaidText,
                        onValueChange = { amountPaidText = it },
                        label = { Text("Amount Paid Now ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    Text("Payment Method:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("CASH" to "Cash", "BANK_TRANSFER" to "Bank", "MTN_MOMO" to "MoMo").forEach { (code, label) ->
                            FilterChip(
                                selected = selectedPaymentMethod == code,
                                onClick = { selectedPaymentMethod = code },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Total Purchase Cost:", fontWeight = FontWeight.Bold)
                                Text("$currency ${String.format(Locale.US, "%.2f", totalPurchaseCost)}", fontWeight = FontWeight.Bold, color = GoldPrimary)
                            }
                        }
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                        Button(
                            onClick = {
                                val p = selectedProduct ?: return@Button
                                val paid = amountPaidText.toDoubleOrNull() ?: totalPurchaseCost
                                onConfirm(
                                    selectedSupplier,
                                    selectedSupplier?.name ?: customSupplierName.ifEmpty { "General Supplier" },
                                    invoiceNumber,
                                    listOf(p to (qty to cost)),
                                    paid,
                                    selectedPaymentMethod,
                                    selectedAccountId,
                                    notes
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            modifier = Modifier.weight(1f).testTag("confirm_purchase_button")
                        ) {
                            Text("Restock & Post", color = NavyDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
