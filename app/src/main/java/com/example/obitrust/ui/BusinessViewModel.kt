package com.example.obitrust.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.obitrust.data.accounting.AccountingEngine
import com.example.obitrust.data.accounting.BalanceSheet
import com.example.obitrust.data.accounting.CashFlowStatement
import com.example.obitrust.data.accounting.ProfitLossStatement
import com.example.obitrust.data.accounting.TrialBalanceSummary
import com.example.obitrust.data.db.AppDatabase
import com.example.obitrust.data.model.*
import com.example.obitrust.data.repository.BusinessRepository
import com.example.obitrust.ui.ai.AiAccountantEngine
import com.example.obitrust.ui.ai.AiChatMessage
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class CartItem(
    val product: ProductEntity,
    var quantity: Int
) {
    val subtotal: Double get() = product.sellingPrice * quantity
    val totalCost: Double get() = product.costPrice * quantity
}

enum class NavigationScreen(val title: String) {
    DASHBOARD("Dashboard"),
    SALES("Sales & POS"),
    INVENTORY("Inventory"),
    PURCHASES("Purchases"),
    EXPENSES("Expenses"),
    ACCOUNTING("Bookkeeping"),
    CUSTOMERS("Customers & Debt"),
    SUPPLIERS("Suppliers"),
    EMPLOYEES("Staff & Payroll"),
    ACCOUNTS("Accounts & MoMo"),
    REPORTS("Reports & Analytics"),
    EBOOKS("E-Book Academy"),
    AI_ACCOUNTANT("AI Accountant"),
    SETTINGS("Settings")
}

class BusinessViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val repository = BusinessRepository(db)

    // Current navigation state
    private val _currentScreen = MutableStateFlow(NavigationScreen.DASHBOARD)
    val currentScreen: StateFlow<NavigationScreen> = _currentScreen.asStateFlow()

    // PIN lock state
    private val _isUnlocked = MutableStateFlow(true)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    // Data streams from repository
    val products = repository.allProducts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val lowStockProducts = repository.lowStockProducts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val outOfStockProducts = repository.outOfStockProducts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sales = repository.allSales.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val purchases = repository.allPurchases.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val expenses = repository.allExpenses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers = repository.allCustomers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val customersWithDebt = repository.customersWithDebt.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suppliers = repository.allSuppliers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val suppliersWithBalance = repository.suppliersWithBalance.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val employees = repository.allEmployees.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val attendance = repository.allAttendance.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts = repository.allAccounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val transfers = repository.allTransfers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val journalEntries = repository.allJournalEntries.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val stockAdjustments = repository.allStockAdjustments.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val auditLogs = repository.recentAuditLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ebooks = repository.allEBooks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val businessProfile = repository.businessProfile.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Sales POS Cart
    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    // AI Assistant Chat Messages
    private val _aiMessages = MutableStateFlow<List<AiChatMessage>>(
        listOf(
            AiChatMessage(
                sender = "AI",
                text = "👋 Welcome to **OBITRUST AI ACCOUNTANT**.\nI am connected to your live business ledger. Ask me about your sales, profits, debts, stock levels, or tap any quick analysis card below!"
            )
        )
    )
    val aiMessages: StateFlow<List<AiChatMessage>> = _aiMessages.asStateFlow()

    // Last completed sale for receipt preview
    private val _lastCompletedSale = MutableStateFlow<Pair<SaleEntity, List<SaleItemEntity>>?>(null)
    val lastCompletedSale: StateFlow<Pair<SaleEntity, List<SaleItemEntity>>?> = _lastCompletedSale.asStateFlow()

    // Accounting State
    private val _journalLines = MutableStateFlow<List<JournalLineEntity>>(emptyList())
    val journalLines: StateFlow<List<JournalLineEntity>> = _journalLines.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
            refreshJournalLines()
        }
    }

    fun setScreen(screen: NavigationScreen) {
        _currentScreen.value = screen
    }

    fun unlockApp(pin: String): Boolean {
        val profile = businessProfile.value
        if (profile == null || !profile.isPinRequired || profile.pinCode == pin) {
            _isUnlocked.value = true
            return true
        }
        return false
    }

    fun lockApp() {
        if (businessProfile.value?.isPinRequired == true) {
            _isUnlocked.value = false
        }
    }

    fun refreshJournalLines() {
        viewModelScope.launch {
            _journalLines.value = repository.getAllJournalLines()
        }
    }

    // --- POS Cart Operations ---
    fun addToCart(product: ProductEntity) {
        val current = _cart.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.product.id == product.id }
        if (existingIndex >= 0) {
            val item = current[existingIndex]
            if (item.quantity < product.quantity) {
                current[existingIndex] = item.copy(quantity = item.quantity + 1)
            }
        } else {
            if (product.quantity > 0) {
                current.add(CartItem(product = product, quantity = 1))
            }
        }
        _cart.value = current
    }

    fun updateCartQuantity(productId: Long, quantity: Int) {
        val current = _cart.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            if (quantity <= 0) {
                current.removeAt(index)
            } else {
                val p = current[index].product
                val validQty = quantity.coerceAtMost(p.quantity)
                current[index] = current[index].copy(quantity = validQty)
            }
            _cart.value = current
        }
    }

    fun removeFromCart(productId: Long) {
        _cart.value = _cart.value.filter { it.product.id != productId }
    }

    fun clearCart() {
        _cart.value = emptyList()
    }

    fun completeCheckout(
        customer: CustomerEntity?,
        discountAmount: Double,
        taxRate: Double,
        paymentMethod: String,
        paymentAccountId: Long?,
        amountPaid: Double,
        notes: String,
        onSuccess: (Long) -> Unit
    ) {
        val cartList = _cart.value
        if (cartList.isEmpty()) return

        val subtotal = cartList.sumOf { it.subtotal }
        val taxAmount = if (taxRate > 0) ((subtotal - discountAmount) * (taxRate / 100.0)).coerceAtLeast(0.0) else 0.0
        val totalAmount = (subtotal - discountAmount + taxAmount).coerceAtLeast(0.0)
        val validPaid = amountPaid.coerceAtMost(totalAmount)
        val balanceDue = (totalAmount - validPaid).coerceAtLeast(0.0)

        val saleNumber = "OB-S-${System.currentTimeMillis() % 1000000}"
        val sale = SaleEntity(
            saleNumber = saleNumber,
            customerId = customer?.id,
            customerName = customer?.name ?: "Walk-in Customer",
            subtotal = subtotal,
            discountAmount = discountAmount,
            taxRate = taxRate,
            taxAmount = taxAmount,
            totalAmount = totalAmount,
            amountPaid = validPaid,
            balanceDue = balanceDue,
            paymentMethod = paymentMethod,
            paymentAccountId = paymentAccountId,
            notes = notes,
            cashierName = businessProfile.value?.activeUserRole ?: "Admin"
        )

        val items = cartList.map {
            SaleItemEntity(
                saleId = 0,
                productId = it.product.id,
                productName = it.product.name,
                quantity = it.quantity,
                unitCost = it.product.costPrice,
                unitPrice = it.product.sellingPrice,
                subtotal = it.subtotal
            )
        }

        viewModelScope.launch {
            val saleId = repository.recordSale(sale, items)
            _lastCompletedSale.value = sale.copy(id = saleId) to items
            clearCart()
            refreshJournalLines()
            onSuccess(saleId)
        }
    }

    fun voidSale(saleId: Long, reason: String) {
        viewModelScope.launch {
            repository.voidSale(saleId, reason)
            refreshJournalLines()
        }
    }

    // --- Purchases ---
    fun recordPurchase(
        supplier: SupplierEntity?,
        supplierName: String,
        invoiceNumber: String,
        items: List<Pair<ProductEntity, Pair<Int, Double>>>, // (product, (qty, costPrice))
        amountPaid: Double,
        paymentMethod: String,
        paymentAccountId: Long?,
        notes: String,
        onSuccess: () -> Unit
    ) {
        if (items.isEmpty()) return
        val total = items.sumOf { it.second.first * it.second.second }
        val paid = amountPaid.coerceAtMost(total)
        val balanceDue = (total - paid).coerceAtLeast(0.0)

        val purchase = PurchaseEntity(
            purchaseNumber = "OB-P-${System.currentTimeMillis() % 1000000}",
            supplierId = supplier?.id,
            supplierName = supplier?.name ?: supplierName,
            invoiceNumber = invoiceNumber,
            totalAmount = total,
            amountPaid = paid,
            balanceDue = balanceDue,
            paymentMethod = paymentMethod,
            paymentAccountId = paymentAccountId,
            notes = notes
        )

        val purchaseItems = items.map { (p, pair) ->
            PurchaseItemEntity(
                purchaseId = 0,
                productId = p.id,
                productName = p.name,
                quantity = pair.first,
                unitCost = pair.second,
                subtotal = pair.first * pair.second
            )
        }

        viewModelScope.launch {
            repository.recordPurchase(purchase, purchaseItems)
            refreshJournalLines()
            onSuccess()
        }
    }

    // --- Expenses ---
    fun recordExpense(
        title: String,
        category: String,
        amount: Double,
        paymentAccountId: Long?,
        paymentMethod: String,
        isRecurring: Boolean,
        notes: String,
        onSuccess: () -> Unit
    ) {
        val exp = ExpenseEntity(
            title = title,
            category = category,
            amount = amount,
            paymentAccountId = paymentAccountId,
            paymentMethod = paymentMethod,
            isRecurring = isRecurring,
            notes = notes
        )
        viewModelScope.launch {
            repository.recordExpense(exp)
            refreshJournalLines()
            onSuccess()
        }
    }

    // --- Customer Debt Payment ---
    fun recordCustomerDebtPayment(
        customerId: Long,
        amount: Double,
        accountId: Long,
        paymentMethod: String,
        notes: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.recordCustomerPayment(customerId, amount, accountId, paymentMethod, notes)
            refreshJournalLines()
            onSuccess()
        }
    }

    // --- Supplier Debt Payment ---
    fun recordSupplierBalancePayment(
        supplierId: Long,
        amount: Double,
        accountId: Long,
        paymentMethod: String,
        notes: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.recordSupplierPayment(supplierId, amount, accountId, paymentMethod, notes)
            refreshJournalLines()
            onSuccess()
        }
    }

    // --- Account Transfer ---
    fun recordAccountTransfer(
        fromAccountId: Long,
        toAccountId: Long,
        amount: Double,
        fee: Double,
        notes: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.recordAccountTransfer(fromAccountId, toAccountId, amount, fee, notes)
            refreshJournalLines()
            onSuccess()
        }
    }

    // --- Stock Adjustment ---
    fun recordStockAdjustment(
        productId: Long,
        newQuantity: Int,
        reason: String,
        notes: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.adjustStock(productId, newQuantity, reason, notes)
            refreshJournalLines()
            onSuccess()
        }
    }

    // --- Product CRUD ---
    fun saveProduct(product: ProductEntity, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.saveProduct(product)
            onSuccess()
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.deleteProduct(product)
        }
    }

    // --- Customer, Supplier, Staff CRUD ---
    fun saveCustomer(customer: CustomerEntity, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.saveCustomer(customer)
            onSuccess()
        }
    }

    fun saveSupplier(supplier: SupplierEntity, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.saveSupplier(supplier)
            onSuccess()
        }
    }

    fun saveEmployee(employee: EmployeeEntity, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.saveEmployee(employee)
            onSuccess()
        }
    }

    fun logAttendance(employee: EmployeeEntity, status: String, notes: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.logAttendance(
                AttendanceEntity(
                    employeeId = employee.id,
                    employeeName = employee.name,
                    status = status,
                    notes = notes
                )
            )
            onSuccess()
        }
    }

    fun saveAccount(account: FinancialAccountEntity, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.saveAccount(account)
            onSuccess()
        }
    }

    fun saveProfile(profile: BusinessProfileEntity, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.saveProfile(profile)
            onSuccess()
        }
    }

    fun updateEBook(ebook: EBookEntity) {
        viewModelScope.launch {
            repository.updateEBook(ebook)
        }
    }

    fun addCustomEBook(title: String, category: String, content: String, summary: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.addCustomEBook(title, category, content, summary)
            onSuccess()
        }
    }

    // --- AI Assistant Queries ---
    fun askAiAssistant(question: String) {
        val userMsg = AiChatMessage(sender = "USER", text = question)
        _aiMessages.value = _aiMessages.value + userMsg

        viewModelScope.launch {
            val allSaleItems = db.saleDao().getAllSaleItems()
            val answer = AiAccountantEngine.analyzeAndAnswer(
                question = question,
                sales = sales.value,
                saleItems = allSaleItems,
                products = products.value,
                expenses = expenses.value,
                customers = customers.value,
                suppliers = suppliers.value,
                accounts = accounts.value,
                journalLines = _journalLines.value,
                currency = businessProfile.value?.currencySymbol ?: "GH₵"
            )
            _aiMessages.value = _aiMessages.value + answer
        }
    }

    // --- Demo Seed Data Generator ---
    fun reseedDemoData(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
            refreshJournalLines()
            onComplete()
        }
    }
}
