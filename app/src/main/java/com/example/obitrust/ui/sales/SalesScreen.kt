package com.example.obitrust.ui.sales

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
import com.example.obitrust.data.model.CustomerEntity
import com.example.obitrust.data.model.ProductEntity
import com.example.obitrust.data.model.SaleEntity
import com.example.obitrust.data.model.SaleItemEntity
import com.example.obitrust.ui.BusinessViewModel
import com.example.obitrust.ui.components.ReceiptDialog
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(
    viewModel: BusinessViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: POS Cashier, 1: Sales History

    val products by viewModel.products.collectAsState()
    val sales by viewModel.sales.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val profile by viewModel.businessProfile.collectAsState()
    val lastCompletedSale by viewModel.lastCompletedSale.collectAsState()

    val currency = profile?.currencySymbol ?: "GH₵"

    var showCheckoutDialog by remember { mutableStateOf(false) }
    var selectedSaleForReceipt by remember { mutableStateOf<Pair<SaleEntity, List<SaleItemEntity>>?>(null) }
    var showBarcodeDialog by remember { mutableStateOf(false) }
    var barcodeInput by remember { mutableStateOf("") }
    var barcodeError by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    // Show receipt for last completed sale if available
    LaunchedEffect(lastCompletedSale) {
        if (lastCompletedSale != null) {
            selectedSaleForReceipt = lastCompletedSale
        }
    }

    Column(modifier = modifier.fillMaxSize().testTag("sales_screen")) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = NavySurface,
            contentColor = GoldPrimary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("POS Cashier (${cart.sumOf { it.quantity }})", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.PointOfSale, contentDescription = null) },
                modifier = Modifier.testTag("pos_tab")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Sales History (${sales.size})", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.History, contentDescription = null) },
                modifier = Modifier.testTag("sales_history_tab")
            )
        }

        if (selectedTab == 0) {
            PosCashierView(
                products = products,
                cart = cart,
                currency = currency,
                onAddToCart = { viewModel.addToCart(it) },
                onUpdateCartQty = { id, qty -> viewModel.updateCartQuantity(id, qty) },
                onRemoveFromCart = { viewModel.removeFromCart(it) },
                onClearCart = { viewModel.clearCart() },
                onOpenCheckout = { showCheckoutDialog = true },
                onOpenBarcode = { showBarcodeDialog = true }
            )
        } else {
            SalesHistoryView(
                sales = sales,
                currency = currency,
                onViewReceipt = { sale ->
                    coroutineScope.launch {
                        val items = viewModel.repository.getSaleItems(sale.id)
                        selectedSaleForReceipt = sale to items
                    }
                },
                onVoidSale = { saleId, reason ->
                    viewModel.voidSale(saleId, reason)
                }
            )
        }
    }

    // Checkout Dialog
    if (showCheckoutDialog && cart.isNotEmpty()) {
        CheckoutDialog(
            cart = cart,
            customers = customers,
            accounts = accounts,
            currency = currency,
            defaultTaxRate = profile?.customTaxRate ?: 4.0,
            onDismiss = { showCheckoutDialog = false },
            onConfirmCheckout = { customer, discount, taxRate, method, accountId, amountPaid, notes ->
                viewModel.completeCheckout(
                    customer = customer,
                    discountAmount = discount,
                    taxRate = taxRate,
                    paymentMethod = method,
                    paymentAccountId = accountId,
                    amountPaid = amountPaid,
                    notes = notes,
                    onSuccess = {
                        showCheckoutDialog = false
                    }
                )
            }
        )
    }

    // Barcode Search Dialog
    if (showBarcodeDialog) {
        AlertDialog(
            onDismissRequest = { showBarcodeDialog = false },
            title = { Text("Barcode Scanner / Search", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Scan product barcode with scanner or enter code manually:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = barcodeInput,
                        onValueChange = {
                            barcodeInput = it
                            barcodeError = null
                        },
                        label = { Text("Barcode / SKU") },
                        leadingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().testTag("barcode_input_field"),
                        singleLine = true
                    )
                    if (barcodeError != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(barcodeError!!, color = CrimsonExpense, style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Quick Test Codes from Inventory:", style = MaterialTheme.typography.labelSmall)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        items(products.filter { it.barcode.isNotEmpty() }.take(5)) { p ->
                            SuggestionChip(
                                onClick = { barcodeInput = p.barcode },
                                label = { Text(p.name.take(14) + " (${p.barcode.takeLast(4)})", fontSize = 11.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val found = products.firstOrNull { it.barcode == barcodeInput.trim() }
                        if (found != null) {
                            viewModel.addToCart(found)
                            showBarcodeDialog = false
                            barcodeInput = ""
                        } else {
                            barcodeError = "No product found with barcode '$barcodeInput'"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                    modifier = Modifier.testTag("scan_find_button")
                ) {
                    Text("Find & Add", color = NavyDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBarcodeDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Receipt Dialog
    selectedSaleForReceipt?.let { (sale, items) ->
        ReceiptDialog(
            sale = sale,
            items = items,
            currency = currency,
            storeName = profile?.businessName ?: "OBITRUST VENTURES GHANA",
            storePhone = profile?.phone ?: "+233 24 123 4567",
            storeAddress = profile?.address ?: "Accra Central, Ghana",
            receiptFooter = profile?.receiptFooter ?: "Thank you for doing business with OBITRUST.",
            logoUri = profile?.logoUri,
            selectedLogoPreset = profile?.selectedLogoPreset,
            onDismiss = { selectedSaleForReceipt = null }
        )
    }
}

@Composable
private fun PosCashierView(
    products: List<ProductEntity>,
    cart: List<com.example.obitrust.ui.CartItem>,
    currency: String,
    onAddToCart: (ProductEntity) -> Unit,
    onUpdateCartQty: (Long, Int) -> Unit,
    onRemoveFromCart: (Long) -> Unit,
    onClearCart: () -> Unit,
    onOpenCheckout: () -> Unit,
    onOpenBarcode: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var showCartSheet by remember { mutableStateOf(false) }

    val categories = remember(products) {
        listOf("All") + products.map { it.category }.distinct()
    }

    val filteredProducts = remember(products, searchQuery, selectedCategory) {
        products.filter { p ->
            val matchCategory = selectedCategory == "All" || p.category == selectedCategory
            val matchQuery = searchQuery.isEmpty() ||
                    p.name.contains(searchQuery, ignoreCase = true) ||
                    p.barcode.contains(searchQuery, ignoreCase = true) ||
                    p.category.contains(searchQuery, ignoreCase = true)
            matchCategory && matchQuery
        }
    }

    val cartTotal = cart.sumOf { it.subtotal }
    val cartCount = cart.sumOf { it.quantity }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search & Barcode row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search product name or SKU...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("pos_search_field"),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onOpenBarcode,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(GoldPrimary)
                        .testTag("barcode_scan_button")
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Barcode", tint = NavyDark)
                }
            }

            // Categories chips
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldPrimary,
                            selectedLabelColor = NavyDark
                        )
                    )
                }
            }

            // Products Catalog List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = if (cart.isNotEmpty()) 80.dp else 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredProducts) { product ->
                    ProductPosCard(
                        product = product,
                        currency = currency,
                        cartQty = cart.firstOrNull { it.product.id == product.id }?.quantity ?: 0,
                        onAdd = { onAddToCart(product) }
                    )
                }
            }
        }

        // Floating Cart Bottom Bar if items in cart
        if (cart.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .testTag("cart_bottom_bar")
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.clickable { showCartSheet = true }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Badge(containerColor = GoldPrimary, contentColor = NavyDark) {
                                Text("$cartCount items", fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "View Cart",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "$currency ${String.format(Locale.US, "%,.2f", cartTotal)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldProfit
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showCartSheet = true },
                            modifier = Modifier.testTag("view_cart_button")
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cart")
                        }

                        Button(
                            onClick = onOpenCheckout,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            modifier = Modifier.testTag("checkout_button")
                        ) {
                            Text("Checkout", color = NavyDark, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = NavyDark, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Cart Modal Sheet
        if (showCartSheet) {
            CartDetailDialog(
                cart = cart,
                currency = currency,
                onUpdateQty = onUpdateCartQty,
                onRemove = onRemoveFromCart,
                onClear = {
                    onClearCart()
                    showCartSheet = false
                },
                onCheckout = {
                    showCartSheet = false
                    onOpenCheckout()
                },
                onDismiss = { showCartSheet = false }
            )
        }
    }
}

@Composable
private fun ProductPosCard(
    product: ProductEntity,
    currency: String,
    cartQty: Int,
    onAdd: () -> Unit
) {
    val isOutOfStock = product.quantity <= 0
    val isLowStock = product.quantity > 0 && product.quantity <= product.minStockLevel

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOutOfStock) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pos_product_${product.id}")
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isOutOfStock) CrimsonExpense.copy(alpha = 0.2f) else GoldPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Inventory,
                    contentDescription = null,
                    tint = if (isOutOfStock) CrimsonExpense else GoldPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$currency ${String.format(Locale.US, "%.2f", product.sellingPrice)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Stock: ${product.quantity}",
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            isOutOfStock -> CrimsonExpense
                            isLowStock -> Color(0xFFF59E0B)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = if (isLowStock || isOutOfStock) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            if (cartQty > 0) {
                Badge(
                    containerColor = EmeraldProfit,
                    contentColor = Color.White,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("$cartQty in cart")
                }
            }

            FilledTonalButton(
                onClick = onAdd,
                enabled = !isOutOfStock && cartQty < product.quantity,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("add_to_cart_${product.id}")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add")
            }
        }
    }
}

@Composable
private fun CartDetailDialog(
    cart: List<com.example.obitrust.ui.CartItem>,
    currency: String,
    onUpdateQty: (Long, Int) -> Unit,
    onRemove: (Long) -> Unit,
    onClear: () -> Unit,
    onCheckout: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(8.dp).testTag("cart_detail_dialog")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Current Cart", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    TextButton(onClick = onClear) {
                        Text("Clear All", color = CrimsonExpense)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false).heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(cart) { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.product.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "$currency ${String.format(Locale.US, "%.2f", item.product.sellingPrice)} each",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { onUpdateQty(item.product.id, item.quantity - 1) }) {
                                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease", tint = CrimsonExpense)
                                }
                                Text("${item.quantity}", fontWeight = FontWeight.Bold)
                                IconButton(onClick = { onUpdateQty(item.product.id, item.quantity + 1) }) {
                                    Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase", tint = EmeraldProfit)
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Amount:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "$currency ${String.format(Locale.US, "%,.2f", cart.sumOf { it.subtotal })}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldProfit
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onCheckout,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                    modifier = Modifier.fillMaxWidth().testTag("proceed_checkout_button")
                ) {
                    Text("Proceed to Payment", color = NavyDark, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CheckoutDialog(
    cart: List<com.example.obitrust.ui.CartItem>,
    customers: List<CustomerEntity>,
    accounts: List<com.example.obitrust.data.model.FinancialAccountEntity>,
    currency: String,
    defaultTaxRate: Double,
    onDismiss: () -> Unit,
    onConfirmCheckout: (CustomerEntity?, Double, Double, String, Long?, Double, String) -> Unit
) {
    var selectedCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var discountText by remember { mutableStateOf("0") }
    var applyTax by remember { mutableStateOf(false) }
    var selectedPaymentMethod by remember { mutableStateOf("CASH") }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull { it.accountType == "CASH" }?.id ?: accounts.firstOrNull()?.id) }
    var amountPaidText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }

    val subtotal = cart.sumOf { it.subtotal }
    val discount = discountText.toDoubleOrNull() ?: 0.0
    val taxRate = if (applyTax) defaultTaxRate else 0.0
    val taxAmount = ((subtotal - discount) * (taxRate / 100.0)).coerceAtLeast(0.0)
    val totalAmount = (subtotal - discount + taxAmount).coerceAtLeast(0.0)

    // Default amount paid to totalAmount
    LaunchedEffect(totalAmount, selectedPaymentMethod) {
        if (amountPaidText.isEmpty() || selectedPaymentMethod != "CREDIT") {
            amountPaidText = String.format(Locale.US, "%.2f", totalAmount)
        }
    }

    val amountPaid = amountPaidText.toDoubleOrNull() ?: totalAmount
    val balanceDue = (totalAmount - amountPaid).coerceAtLeast(0.0)

    val paymentMethods = listOf(
        "CASH" to "Cash",
        "MTN_MOMO" to "MTN MoMo",
        "TELECEL_CASH" to "Telecel Cash",
        "AT_MONEY" to "AT Money",
        "BANK_TRANSFER" to "Bank Transfer",
        "CREDIT" to "Credit Sale (Debtor)"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(4.dp).testTag("checkout_modal")
        ) {
            LazyColumn(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text("Complete Sale", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Select customer, payment channel and finalize transaction", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Customer picker
                item {
                    Text("Customer:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = selectedCustomer == null,
                                onClick = { selectedCustomer = null },
                                label = { Text("Walk-in Customer") }
                            )
                        }
                        items(customers) { c ->
                            FilterChip(
                                selected = selectedCustomer?.id == c.id,
                                onClick = { selectedCustomer = c },
                                label = { Text(c.name) }
                            )
                        }
                    }
                }

                // Discount and Tax
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = discountText,
                            onValueChange = { discountText = it },
                            label = { Text("Discount ($currency)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.weight(1f).clickable { applyTax = !applyTax }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = applyTax, onCheckedChange = { applyTax = it })
                                Text("Tax ($defaultTaxRate%)", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                // Payment Method Selector
                item {
                    Text("Payment Method:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        paymentMethods.forEach { (code, name) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedPaymentMethod = code
                                        if (code == "CREDIT") {
                                            amountPaidText = "0.00"
                                        }
                                    }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedPaymentMethod == code,
                                    onClick = {
                                        selectedPaymentMethod = code
                                        if (code == "CREDIT") amountPaidText = "0.00"
                                    }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(name, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                // Receiving Account
                item {
                    Text("Deposit Account:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
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

                // Amount Paid field
                item {
                    OutlinedTextField(
                        value = amountPaidText,
                        onValueChange = { amountPaidText = it },
                        label = { Text("Amount Paid ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("amount_paid_input"),
                        singleLine = true
                    )
                }

                // Summary Card
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Subtotal:", style = MaterialTheme.typography.bodySmall)
                                Text("$currency ${String.format(Locale.US, "%.2f", subtotal)}")
                            }
                            if (discount > 0) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Discount:", style = MaterialTheme.typography.bodySmall)
                                    Text("-$currency ${String.format(Locale.US, "%.2f", discount)}", color = CrimsonExpense)
                                }
                            }
                            if (taxAmount > 0) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Tax ($defaultTaxRate%):", style = MaterialTheme.typography.bodySmall)
                                    Text("+$currency ${String.format(Locale.US, "%.2f", taxAmount)}")
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Total Amount:", fontWeight = FontWeight.Bold)
                                Text("$currency ${String.format(Locale.US, "%.2f", totalAmount)}", fontWeight = FontWeight.Bold, color = EmeraldProfit)
                            }
                            if (balanceDue > 0) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Balance Due (Customer Debt):", style = MaterialTheme.typography.bodySmall, color = CrimsonExpense)
                                    Text("$currency ${String.format(Locale.US, "%.2f", balanceDue)}", color = CrimsonExpense, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                onConfirmCheckout(
                                    selectedCustomer,
                                    discount,
                                    taxRate,
                                    selectedPaymentMethod,
                                    selectedAccountId,
                                    amountPaid,
                                    notesText
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            modifier = Modifier.weight(1f).testTag("finalize_sale_button")
                        ) {
                            Text("Finalize & Receipt", color = NavyDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SalesHistoryView(
    sales: List<SaleEntity>,
    currency: String,
    onViewReceipt: (SaleEntity) -> Unit,
    onVoidSale: (Long, String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var saleToVoid by remember { mutableStateOf<SaleEntity?>(null) }
    var voidReason by remember { mutableStateOf("Customer returned goods / mistaken order") }

    val sdf = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    val filteredSales = remember(sales, searchQuery) {
        sales.filter {
            searchQuery.isEmpty() ||
                    it.saleNumber.contains(searchQuery, ignoreCase = true) ||
                    it.customerName.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by receipt # or customer name...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("sales_history_search")
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (filteredSales.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No sales records found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredSales) { sale ->
                    val isVoided = sale.status == "VOIDED"
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isVoided) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onViewReceipt(sale) }
                            .testTag("sale_card_${sale.id}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("#${sale.saleNumber}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    if (isVoided) {
                                        Badge(containerColor = CrimsonExpense, contentColor = Color.White) {
                                            Text("VOIDED", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Badge(containerColor = EmeraldProfit.copy(alpha = 0.2f), contentColor = EmeraldProfit) {
                                            Text(sale.paymentMethod, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Text(
                                    "$currency ${String.format(Locale.US, "%,.2f", sale.totalAmount)}",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isVoided) MaterialTheme.colorScheme.onSurfaceVariant else EmeraldProfit,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Customer: ${sale.customerName}", style = MaterialTheme.typography.bodySmall)
                                Text(sdf.format(Date(sale.createdAt)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            if (sale.balanceDue > 0 && !isVoided) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Unpaid Debt: $currency ${String.format(Locale.US, "%.2f", sale.balanceDue)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CrimsonExpense,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = { onViewReceipt(sale) },
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Receipt")
                                }
                                if (!isVoided) {
                                    TextButton(
                                        onClick = { saleToVoid = sale },
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Icon(Icons.Default.Cancel, contentDescription = null, tint = CrimsonExpense, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Void Sale", color = CrimsonExpense)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Void Sale Confirmation Dialog
    saleToVoid?.let { s ->
        AlertDialog(
            onDismissRequest = { saleToVoid = null },
            title = { Text("Void Sale #${s.saleNumber}?", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Voiding will automatically RESTORE inventory stock quantities and cancel financial revenue entries in the general ledger.")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = voidReason,
                        onValueChange = { voidReason = it },
                        label = { Text("Reason for Voiding") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onVoidSale(s.id, voidReason)
                        saleToVoid = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonExpense)
                ) {
                    Text("Confirm Void", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { saleToVoid = null }) { Text("Cancel") }
            }
        )
    }
}
