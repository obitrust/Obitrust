package com.example.obitrust.ui.inventory

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.obitrust.data.model.ProductEntity
import com.example.obitrust.ui.BusinessViewModel
import com.example.obitrust.ui.components.MetricCard
import com.example.obitrust.ui.components.SectionHeader
import com.example.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: BusinessViewModel,
    modifier: Modifier = Modifier
) {
    val products by viewModel.products.collectAsState()
    val lowStock by viewModel.lowStockProducts.collectAsState()
    val outOfStock by viewModel.outOfStockProducts.collectAsState()
    val profile by viewModel.businessProfile.collectAsState()

    val currency = profile?.currencySymbol ?: "GH₵"

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") } // "All", "Low Stock", "Out of Stock"
    var selectedCategory by remember { mutableStateOf("All") }

    var showAddEditDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<ProductEntity?>(null) }

    var showAdjustDialog by remember { mutableStateOf(false) }
    var productToAdjust by remember { mutableStateOf<ProductEntity?>(null) }

    val categories = remember(products) {
        listOf("All") + products.map { it.category }.distinct()
    }

    val filteredProducts = remember(products, searchQuery, selectedFilter, selectedCategory) {
        products.filter { p ->
            val matchFilter = when (selectedFilter) {
                "Low Stock" -> p.quantity > 0 && p.quantity <= p.minStockLevel
                "Out of Stock" -> p.quantity <= 0
                else -> true
            }
            val matchCategory = selectedCategory == "All" || p.category == selectedCategory
            val matchQuery = searchQuery.isEmpty() ||
                    p.name.contains(searchQuery, ignoreCase = true) ||
                    p.barcode.contains(searchQuery, ignoreCase = true) ||
                    p.supplierName.contains(searchQuery, ignoreCase = true)
            matchFilter && matchCategory && matchQuery
        }
    }

    val totalCostValue = products.sumOf { it.costPrice * it.quantity }
    val totalRetailValue = products.sumOf { it.sellingPrice * it.quantity }
    val projectedProfit = totalRetailValue - totalCostValue

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    productToEdit = null
                    showAddEditDialog = true
                },
                containerColor = GoldPrimary,
                contentColor = NavyDark,
                modifier = Modifier.testTag("add_product_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Product")
            }
        },
        modifier = modifier.fillMaxSize().testTag("inventory_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Stock Valuation Summary
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = NavySurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavyBorder)),
                    modifier = Modifier.fillMaxWidth().testTag("stock_valuation_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Inventory Stock Valuation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Badge(containerColor = GoldPrimary.copy(alpha = 0.2f), contentColor = GoldPrimary) {
                                Text("${products.size} Products", fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Cost Value", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "$currency ${String.format(Locale.US, "%,.2f", totalCostValue)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Retail Value", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "$currency ${String.format(Locale.US, "%,.2f", totalRetailValue)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldProfit
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Projected Margin", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "$currency ${String.format(Locale.US, "%,.2f", projectedProfit)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name, barcode or supplier...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("inventory_search_field")
                )
            }

            // Filter Chips (All, Low Stock, Out of Stock)
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedFilter == "All",
                        onClick = { selectedFilter = "All" },
                        label = { Text("All Products (${products.size})") }
                    )
                    FilterChip(
                        selected = selectedFilter == "Low Stock",
                        onClick = { selectedFilter = "Low Stock" },
                        label = { Text("Low Stock (${lowStock.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFF59E0B),
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = selectedFilter == "Out of Stock",
                        onClick = { selectedFilter = "Out of Stock" },
                        label = { Text("Out of Stock (${outOfStock.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CrimsonExpense,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Category Chips
            item {
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

            // Products List
            items(filteredProducts) { product ->
                val isOut = product.quantity <= 0
                val isLow = product.quantity > 0 && product.quantity <= product.minStockLevel

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth().testTag("product_row_${product.id}")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    product.name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${product.category} • SKU: ${product.barcode.ifEmpty { "N/A" }}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Badge(
                                containerColor = when {
                                    isOut -> CrimsonExpense
                                    isLow -> Color(0xFFF59E0B)
                                    else -> EmeraldProfit.copy(alpha = 0.2f)
                                },
                                contentColor = when {
                                    isOut || isLow -> Color.White
                                    else -> EmeraldProfit
                                }
                            ) {
                                Text(
                                    text = when {
                                        isOut -> "OUT OF STOCK"
                                        isLow -> "${product.quantity} LEFT (LOW)"
                                        else -> "${product.quantity} in stock"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Cost: $currency ${String.format(Locale.US, "%.2f", product.costPrice)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Price: $currency ${String.format(Locale.US, "%.2f", product.sellingPrice)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = GoldPrimary
                            )
                            val margin = if (product.sellingPrice > 0) ((product.sellingPrice - product.costPrice) / product.sellingPrice) * 100 else 0.0
                            Text(
                                "Margin: ${String.format(Locale.US, "%.0f%%", margin)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldProfit
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        Row(
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(
                                onClick = {
                                    productToAdjust = product
                                    showAdjustDialog = true
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Adjust Stock")
                            }

                            TextButton(
                                onClick = {
                                    productToEdit = product
                                    showAddEditDialog = true
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit")
                            }

                            IconButton(
                                onClick = { viewModel.deleteProduct(product) }
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = CrimsonExpense, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Product Dialog
    if (showAddEditDialog) {
        AddEditProductDialog(
            product = productToEdit,
            currency = currency,
            onDismiss = { showAddEditDialog = false },
            onSave = { p ->
                viewModel.saveProduct(p) {
                    showAddEditDialog = false
                }
            }
        )
    }

    // Stock Adjustment Dialog
    productToAdjust?.let { p ->
        StockAdjustmentDialog(
            product = p,
            onDismiss = { productToAdjust = null },
            onConfirm = { newQty, reason, notes ->
                viewModel.recordStockAdjustment(p.id, newQty, reason, notes) {
                    productToAdjust = null
                }
            }
        )
    }
}

@Composable
private fun AddEditProductDialog(
    product: ProductEntity?,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (ProductEntity) -> Unit
) {
    var name by remember { mutableStateOf(product?.name ?: "") }
    var category by remember { mutableStateOf(product?.category ?: "Groceries") }
    var barcode by remember { mutableStateOf(product?.barcode ?: "") }
    var costPriceText by remember { mutableStateOf(product?.costPrice?.toString() ?: "") }
    var sellingPriceText by remember { mutableStateOf(product?.sellingPrice?.toString() ?: "") }
    var quantityText by remember { mutableStateOf(product?.quantity?.toString() ?: "10") }
    var minStockText by remember { mutableStateOf(product?.minStockLevel?.toString() ?: "5") }
    var supplierName by remember { mutableStateOf(product?.supplierName ?: "") }
    var storageLocation by remember { mutableStateOf(product?.storageLocation ?: "Main Store") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(4.dp).testTag("add_product_dialog")
        ) {
            LazyColumn(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        if (product == null) "Add New Product" else "Edit Product",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Product Name *") },
                        modifier = Modifier.fillMaxWidth().testTag("product_name_input"),
                        singleLine = true
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("Category") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = barcode,
                            onValueChange = { barcode = it },
                            label = { Text("Barcode / SKU") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = costPriceText,
                            onValueChange = { costPriceText = it },
                            label = { Text("Cost Price ($currency)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = sellingPriceText,
                            onValueChange = { sellingPriceText = it },
                            label = { Text("Selling Price ($currency)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = quantityText,
                            onValueChange = { quantityText = it },
                            label = { Text("Quantity Available") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = minStockText,
                            onValueChange = { minStockText = it },
                            label = { Text("Min Stock Alert") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = supplierName,
                            onValueChange = { supplierName = it },
                            label = { Text("Supplier Name") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = storageLocation,
                            onValueChange = { storageLocation = it },
                            label = { Text("Storage Location") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                if (name.isNotBlank()) {
                                    val cost = costPriceText.toDoubleOrNull() ?: 0.0
                                    val sell = sellingPriceText.toDoubleOrNull() ?: cost
                                    val qty = quantityText.toIntOrNull() ?: 0
                                    val minQty = minStockText.toIntOrNull() ?: 5
                                    onSave(
                                        ProductEntity(
                                            id = product?.id ?: 0,
                                            name = name.trim(),
                                            category = category.trim().ifEmpty { "General Goods" },
                                            barcode = barcode.trim(),
                                            costPrice = cost,
                                            sellingPrice = sell,
                                            quantity = qty,
                                            minStockLevel = minQty,
                                            supplierName = supplierName.trim(),
                                            storageLocation = storageLocation.trim()
                                        )
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            modifier = Modifier.weight(1f).testTag("save_product_button")
                        ) {
                            Text("Save Product", color = NavyDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StockAdjustmentDialog(
    product: ProductEntity,
    onDismiss: () -> Unit,
    onConfirm: (Int, String, String) -> Unit
) {
    var newQtyText by remember { mutableStateOf(product.quantity.toString()) }
    var selectedReason by remember { mutableStateOf("DAMAGED") }
    var notes by remember { mutableStateOf("") }

    val reasons = listOf(
        "DAMAGED" to "Damaged / Broken",
        "LOST/MISSING" to "Lost / Missing",
        "EXPIRED" to "Expired Product",
        "AUDIT_CORRECTION" to "Physical Audit Correction",
        "RETURN" to "Supplier / Customer Return",
        "BRANCH_TRANSFER" to "Transfer to Branch"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(4.dp).testTag("stock_adjustment_dialog")
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Adjust Stock: ${product.name}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Current registered quantity: ${product.quantity}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                OutlinedTextField(
                    value = newQtyText,
                    onValueChange = { newQtyText = it },
                    label = { Text("New Actual Count") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("adjustment_qty_input"),
                    singleLine = true
                )

                Text("Reason for adjustment:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    reasons.forEach { (code, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { selectedReason = code }
                        ) {
                            RadioButton(selected = selectedReason == code, onClick = { selectedReason = code })
                            Text(label, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Audit Notes / Memo") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val newQty = newQtyText.toIntOrNull() ?: product.quantity
                            onConfirm(newQty, selectedReason, notes)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                        modifier = Modifier.weight(1f).testTag("confirm_adjustment_button")
                    ) {
                        Text("Save & Reconcile", color = NavyDark, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
