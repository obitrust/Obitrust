package com.example.obitrust.data.repository

import com.example.obitrust.data.accounting.ChartOfAccounts
import com.example.obitrust.data.db.AppDatabase
import com.example.obitrust.data.ebooks.EBookCurriculum
import com.example.obitrust.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BusinessRepository(private val db: AppDatabase) {

    val allProducts: Flow<List<ProductEntity>> = db.productDao().getAllProducts()
    val lowStockProducts: Flow<List<ProductEntity>> = db.productDao().getLowStockProducts()
    val outOfStockProducts: Flow<List<ProductEntity>> = db.productDao().getOutOfStockProducts()

    val allSales: Flow<List<SaleEntity>> = db.saleDao().getAllSales()
    val allPurchases: Flow<List<PurchaseEntity>> = db.purchaseDao().getAllPurchases()
    val allExpenses: Flow<List<ExpenseEntity>> = db.expenseDao().getAllExpenses()

    val allCustomers: Flow<List<CustomerEntity>> = db.customerDao().getAllCustomers()
    val customersWithDebt: Flow<List<CustomerEntity>> = db.customerDao().getCustomersWithDebt()

    val allSuppliers: Flow<List<SupplierEntity>> = db.supplierDao().getAllSuppliers()
    val suppliersWithBalance: Flow<List<SupplierEntity>> = db.supplierDao().getSuppliersWithBalance()

    val allEmployees: Flow<List<EmployeeEntity>> = db.employeeDao().getAllEmployees()
    val allAttendance: Flow<List<AttendanceEntity>> = db.employeeDao().getAllAttendance()

    val allAccounts: Flow<List<FinancialAccountEntity>> = db.financialAccountDao().getAllAccounts()
    val allTransfers: Flow<List<AccountTransferEntity>> = db.financialAccountDao().getAllTransfers()

    val allJournalEntries: Flow<List<JournalEntryEntity>> = db.accountingDao().getAllJournalEntries()
    val allStockAdjustments: Flow<List<StockAdjustmentEntity>> = db.stockAdjustmentDao().getAllAdjustments()
    val recentAuditLogs: Flow<List<AuditLogEntity>> = db.auditLogDao().getRecentLogs()

    val allEBooks: Flow<List<EBookEntity>> = db.eBookDao().getAllEBooks()
    val businessProfile: Flow<BusinessProfileEntity?> = db.businessProfileDao().getProfile()

    suspend fun getSaleItems(saleId: Long): List<SaleItemEntity> = withContext(Dispatchers.IO) {
        db.saleDao().getSaleItems(saleId)
    }

    suspend fun getPurchaseItems(purchaseId: Long): List<PurchaseItemEntity> = withContext(Dispatchers.IO) {
        db.purchaseDao().getPurchaseItems(purchaseId)
    }

    suspend fun getAllJournalLines(): List<JournalLineEntity> = withContext(Dispatchers.IO) {
        db.accountingDao().getAllJournalLines()
    }

    suspend fun getLinesForEntry(entryId: Long): List<JournalLineEntity> = withContext(Dispatchers.IO) {
        db.accountingDao().getLinesForEntry(entryId)
    }

    suspend fun getProductByBarcode(barcode: String): ProductEntity? = withContext(Dispatchers.IO) {
        db.productDao().getProductByBarcode(barcode)
    }

    // --- Record Sale with Automatic Double-Entry Journaling & Inventory Deduction ---
    suspend fun recordSale(
        sale: SaleEntity,
        items: List<SaleItemEntity>
    ): Long = withContext(Dispatchers.IO) {
        val saleId = db.saleDao().insertSale(sale)
        val itemsWithId = items.map { it.copy(saleId = saleId) }
        db.saleDao().insertSaleItems(itemsWithId)

        // Decrement stock for each item
        var totalCost = 0.0
        for (item in items) {
            db.productDao().decrementStock(item.productId, item.quantity)
            totalCost += (item.unitCost * item.quantity)
        }

        // Update customer debt if credit sale
        if (sale.customerId != null && sale.balanceDue > 0) {
            db.customerDao().addDebt(sale.customerId, sale.balanceDue)
        }

        // Update financial account if paid
        if (sale.paymentAccountId != null && sale.amountPaid > 0) {
            db.financialAccountDao().creditAccount(sale.paymentAccountId, sale.amountPaid)
        }

        // Double-Entry Bookkeeping
        val jeId = db.accountingDao().insertJournalEntry(
            JournalEntryEntity(
                entryNumber = "JE-S-${System.currentTimeMillis() % 100000}",
                transactionType = "SALE",
                referenceId = saleId,
                referenceCode = sale.saleNumber,
                description = "Sale to ${sale.customerName} via ${sale.paymentMethod}"
            )
        )

        val paymentAccountCode = when (sale.paymentMethod) {
            "BANK_TRANSFER" -> ChartOfAccounts.CODE_BANK
            "MTN_MOMO", "TELECEL_CASH", "AT_MONEY" -> ChartOfAccounts.CODE_MOMO
            "CREDIT" -> ChartOfAccounts.CODE_ACCOUNTS_RECEIVABLE
            else -> ChartOfAccounts.CODE_CASH
        }
        val paymentAccountName = when (sale.paymentMethod) {
            "BANK_TRANSFER" -> ChartOfAccounts.NAME_BANK
            "MTN_MOMO", "TELECEL_CASH", "AT_MONEY" -> "${sale.paymentMethod} Wallet"
            "CREDIT" -> ChartOfAccounts.NAME_ACCOUNTS_RECEIVABLE
            else -> ChartOfAccounts.NAME_CASH
        }

        val lines = mutableListOf<JournalLineEntity>()
        if (sale.amountPaid > 0) {
            lines.add(
                JournalLineEntity(
                    journalEntryId = jeId,
                    accountCode = paymentAccountCode,
                    accountName = paymentAccountName,
                    debit = sale.amountPaid,
                    credit = 0.0
                )
            )
        }
        if (sale.balanceDue > 0) {
            lines.add(
                JournalLineEntity(
                    journalEntryId = jeId,
                    accountCode = ChartOfAccounts.CODE_ACCOUNTS_RECEIVABLE,
                    accountName = ChartOfAccounts.NAME_ACCOUNTS_RECEIVABLE,
                    debit = sale.balanceDue,
                    credit = 0.0
                )
            )
        }
        lines.add(
            JournalLineEntity(
                journalEntryId = jeId,
                accountCode = ChartOfAccounts.CODE_SALES_REVENUE,
                accountName = ChartOfAccounts.NAME_SALES_REVENUE,
                debit = 0.0,
                credit = sale.subtotal - sale.discountAmount
            )
        )
        if (sale.taxAmount > 0) {
            lines.add(
                JournalLineEntity(
                    journalEntryId = jeId,
                    accountCode = ChartOfAccounts.CODE_TAX_PAYABLE,
                    accountName = ChartOfAccounts.NAME_TAX_PAYABLE,
                    debit = 0.0,
                    credit = sale.taxAmount
                )
            )
        }

        // COGS and Inventory Asset
        if (totalCost > 0) {
            lines.add(
                JournalLineEntity(
                    journalEntryId = jeId,
                    accountCode = ChartOfAccounts.CODE_COGS,
                    accountName = ChartOfAccounts.NAME_COGS,
                    debit = totalCost,
                    credit = 0.0
                )
            )
            lines.add(
                JournalLineEntity(
                    journalEntryId = jeId,
                    accountCode = ChartOfAccounts.CODE_INVENTORY,
                    accountName = ChartOfAccounts.NAME_INVENTORY,
                    debit = 0.0,
                    credit = totalCost
                )
            )
        }

        db.accountingDao().insertJournalLines(lines)

        db.auditLogDao().insertLog(
            AuditLogEntity(
                action = "RECORD_SALE",
                module = "SALES",
                details = "Completed sale #${sale.saleNumber} for total GH₵ ${String.format(Locale.US, "%.2f", sale.totalAmount)}"
            )
        )

        saleId
    }

    // --- Void / Cancel Sale with Stock Restoration ---
    suspend fun voidSale(saleId: Long, reason: String) = withContext(Dispatchers.IO) {
        val sale = db.saleDao().getSaleById(saleId) ?: return@withContext
        if (sale.status == "VOIDED") return@withContext

        db.saleDao().voidSale(saleId)
        val items = db.saleDao().getSaleItems(saleId)

        // Restore stock
        for (item in items) {
            db.productDao().incrementStock(item.productId, item.quantity)
        }

        // Reverse financial account if paid
        if (sale.paymentAccountId != null && sale.amountPaid > 0) {
            db.financialAccountDao().debitAccount(sale.paymentAccountId, sale.amountPaid)
        }

        // Reverse customer debt if credit
        if (sale.customerId != null && sale.balanceDue > 0) {
            db.customerDao().payDebt(sale.customerId, sale.balanceDue)
        }

        db.auditLogDao().insertLog(
            AuditLogEntity(
                action = "VOID_SALE",
                module = "SALES",
                details = "Voided sale #${sale.saleNumber}. Reason: $reason. Stock restored."
            )
        )
    }

    // --- Record Purchase with Automatic Stock Increment & AP Tracking ---
    suspend fun recordPurchase(
        purchase: PurchaseEntity,
        items: List<PurchaseItemEntity>
    ): Long = withContext(Dispatchers.IO) {
        val purchaseId = db.purchaseDao().insertPurchase(purchase)
        val itemsWithId = items.map { it.copy(purchaseId = purchaseId) }
        db.purchaseDao().insertPurchaseItems(itemsWithId)

        // Increment stock
        for (item in items) {
            db.productDao().incrementStock(item.productId, item.quantity)
        }

        // Update supplier balance if credit
        if (purchase.supplierId != null && purchase.balanceDue > 0) {
            db.supplierDao().addBalance(purchase.supplierId, purchase.balanceDue)
        }

        // Debit financial account for amount paid
        if (purchase.paymentAccountId != null && purchase.amountPaid > 0) {
            db.financialAccountDao().debitAccount(purchase.paymentAccountId, purchase.amountPaid)
        }

        // Double-entry Journal
        val jeId = db.accountingDao().insertJournalEntry(
            JournalEntryEntity(
                entryNumber = "JE-P-${System.currentTimeMillis() % 100000}",
                transactionType = "PURCHASE",
                referenceId = purchaseId,
                referenceCode = purchase.purchaseNumber,
                description = "Stock purchase from ${purchase.supplierName}"
            )
        )

        val paymentAccountCode = when (purchase.paymentMethod) {
            "BANK_TRANSFER" -> ChartOfAccounts.CODE_BANK
            "MTN_MOMO", "TELECEL_CASH", "AT_MONEY" -> ChartOfAccounts.CODE_MOMO
            else -> ChartOfAccounts.CODE_CASH
        }
        val paymentAccountName = when (purchase.paymentMethod) {
            "BANK_TRANSFER" -> ChartOfAccounts.NAME_BANK
            "MTN_MOMO", "TELECEL_CASH", "AT_MONEY" -> "${purchase.paymentMethod} Wallet"
            else -> ChartOfAccounts.NAME_CASH
        }

        val lines = mutableListOf<JournalLineEntity>()
        // Debit Inventory Asset
        lines.add(
            JournalLineEntity(
                journalEntryId = jeId,
                accountCode = ChartOfAccounts.CODE_INVENTORY,
                accountName = ChartOfAccounts.NAME_INVENTORY,
                debit = purchase.totalAmount,
                credit = 0.0
            )
        )
        // Credit Cash/Bank/MoMo for amount paid
        if (purchase.amountPaid > 0) {
            lines.add(
                JournalLineEntity(
                    journalEntryId = jeId,
                    accountCode = paymentAccountCode,
                    accountName = paymentAccountName,
                    debit = 0.0,
                    credit = purchase.amountPaid
                )
            )
        }
        // Credit Accounts Payable for balance due
        if (purchase.balanceDue > 0) {
            lines.add(
                JournalLineEntity(
                    journalEntryId = jeId,
                    accountCode = ChartOfAccounts.CODE_ACCOUNTS_PAYABLE,
                    accountName = ChartOfAccounts.NAME_ACCOUNTS_PAYABLE,
                    debit = 0.0,
                    credit = purchase.balanceDue
                )
            )
        }

        db.accountingDao().insertJournalLines(lines)

        db.auditLogDao().insertLog(
            AuditLogEntity(
                action = "RECORD_PURCHASE",
                module = "PURCHASES",
                details = "Stock purchase #${purchase.purchaseNumber} from ${purchase.supplierName} for GH₵ ${String.format(Locale.US, "%.2f", purchase.totalAmount)}"
            )
        )

        purchaseId
    }

    // --- Record Expense with Automatic Journaling ---
    suspend fun recordExpense(expense: ExpenseEntity): Long = withContext(Dispatchers.IO) {
        val expenseId = db.expenseDao().insertExpense(expense)

        if (expense.paymentAccountId != null && expense.amount > 0) {
            db.financialAccountDao().debitAccount(expense.paymentAccountId, expense.amount)
        }

        val (expCode, expName) = ChartOfAccounts.getExpenseCodeAndName(expense.category)
        val jeId = db.accountingDao().insertJournalEntry(
            JournalEntryEntity(
                entryNumber = "JE-E-${System.currentTimeMillis() % 100000}",
                transactionType = "EXPENSE",
                referenceId = expenseId,
                referenceCode = "EXP-$expenseId",
                description = "Expense: ${expense.title} (${expense.category})"
            )
        )

        val paymentAccountCode = when (expense.paymentMethod) {
            "BANK_TRANSFER" -> ChartOfAccounts.CODE_BANK
            "MTN_MOMO", "TELECEL_CASH", "AT_MONEY" -> ChartOfAccounts.CODE_MOMO
            else -> ChartOfAccounts.CODE_CASH
        }
        val paymentAccountName = when (expense.paymentMethod) {
            "BANK_TRANSFER" -> ChartOfAccounts.NAME_BANK
            "MTN_MOMO", "TELECEL_CASH", "AT_MONEY" -> "${expense.paymentMethod} Wallet"
            else -> ChartOfAccounts.NAME_CASH
        }

        db.accountingDao().insertJournalLines(
            listOf(
                JournalLineEntity(
                    journalEntryId = jeId,
                    accountCode = expCode,
                    accountName = expName,
                    debit = expense.amount,
                    credit = 0.0
                ),
                JournalLineEntity(
                    journalEntryId = jeId,
                    accountCode = paymentAccountCode,
                    accountName = paymentAccountName,
                    debit = 0.0,
                    credit = expense.amount
                )
            )
        )

        db.auditLogDao().insertLog(
            AuditLogEntity(
                action = "RECORD_EXPENSE",
                module = "EXPENSES",
                details = "Logged expense '${expense.title}' of GH₵ ${String.format(Locale.US, "%.2f", expense.amount)} in ${expense.category}"
            )
        )

        expenseId
    }

    // --- Customer Debt Repayment ---
    suspend fun recordCustomerPayment(
        customerId: Long,
        amount: Double,
        accountId: Long,
        paymentMethod: String,
        notes: String
    ) = withContext(Dispatchers.IO) {
        val customer = db.customerDao().getCustomerById(customerId) ?: return@withContext
        db.customerDao().payDebt(customerId, amount)
        db.financialAccountDao().creditAccount(accountId, amount)

        val jeId = db.accountingDao().insertJournalEntry(
            JournalEntryEntity(
                entryNumber = "JE-CR-${System.currentTimeMillis() % 100000}",
                transactionType = "CUSTOMER_PAYMENT",
                referenceId = customerId,
                referenceCode = "CUST-$customerId",
                description = "Debt payment from ${customer.name} via $paymentMethod: $notes"
            )
        )

        val paymentAccountCode = when (paymentMethod) {
            "BANK_TRANSFER" -> ChartOfAccounts.CODE_BANK
            "MTN_MOMO", "TELECEL_CASH", "AT_MONEY" -> ChartOfAccounts.CODE_MOMO
            else -> ChartOfAccounts.CODE_CASH
        }

        db.accountingDao().insertJournalLines(
            listOf(
                JournalLineEntity(
                    journalEntryId = jeId,
                    accountCode = paymentAccountCode,
                    accountName = "$paymentMethod Account",
                    debit = amount,
                    credit = 0.0
                ),
                JournalLineEntity(
                    journalEntryId = jeId,
                    accountCode = ChartOfAccounts.CODE_ACCOUNTS_RECEIVABLE,
                    accountName = ChartOfAccounts.NAME_ACCOUNTS_RECEIVABLE,
                    debit = 0.0,
                    credit = amount
                )
            )
        )

        db.auditLogDao().insertLog(
            AuditLogEntity(
                action = "CUSTOMER_PAYMENT",
                module = "CUSTOMERS",
                details = "Received debt payment of GH₵ ${String.format(Locale.US, "%.2f", amount)} from ${customer.name}"
            )
        )
    }

    // --- Supplier Debt Repayment ---
    suspend fun recordSupplierPayment(
        supplierId: Long,
        amount: Double,
        accountId: Long,
        paymentMethod: String,
        notes: String
    ) = withContext(Dispatchers.IO) {
        val supplier = db.supplierDao().getSupplierById(supplierId) ?: return@withContext
        db.supplierDao().payBalance(supplierId, amount)
        db.financialAccountDao().debitAccount(accountId, amount)

        val jeId = db.accountingDao().insertJournalEntry(
            JournalEntryEntity(
                entryNumber = "JE-CP-${System.currentTimeMillis() % 100000}",
                transactionType = "SUPPLIER_PAYMENT",
                referenceId = supplierId,
                referenceCode = "SUPP-$supplierId",
                description = "Payment to ${supplier.name} via $paymentMethod: $notes"
            )
        )

        val paymentAccountCode = when (paymentMethod) {
            "BANK_TRANSFER" -> ChartOfAccounts.CODE_BANK
            "MTN_MOMO", "TELECEL_CASH", "AT_MONEY" -> ChartOfAccounts.CODE_MOMO
            else -> ChartOfAccounts.CODE_CASH
        }

        db.accountingDao().insertJournalLines(
            listOf(
                JournalLineEntity(
                    journalEntryId = jeId,
                    accountCode = ChartOfAccounts.CODE_ACCOUNTS_PAYABLE,
                    accountName = ChartOfAccounts.NAME_ACCOUNTS_PAYABLE,
                    debit = amount,
                    credit = 0.0
                ),
                JournalLineEntity(
                    journalEntryId = jeId,
                    accountCode = paymentAccountCode,
                    accountName = "$paymentMethod Account",
                    debit = 0.0,
                    credit = amount
                )
            )
        )

        db.auditLogDao().insertLog(
            AuditLogEntity(
                action = "SUPPLIER_PAYMENT",
                module = "SUPPLIERS",
                details = "Paid GH₵ ${String.format(Locale.US, "%.2f", amount)} to ${supplier.name}"
            )
        )
    }

    // --- Account Transfer with Fee Logging ---
    suspend fun recordAccountTransfer(
        fromAccountId: Long,
        toAccountId: Long,
        amount: Double,
        fee: Double,
        notes: String
    ) = withContext(Dispatchers.IO) {
        val fromAcc = db.financialAccountDao().getAccountById(fromAccountId) ?: return@withContext
        val toAcc = db.financialAccountDao().getAccountById(toAccountId) ?: return@withContext

        db.financialAccountDao().debitAccount(fromAccountId, amount + fee)
        db.financialAccountDao().creditAccount(toAccountId, amount)

        db.financialAccountDao().insertTransfer(
            AccountTransferEntity(
                fromAccountId = fromAccountId,
                fromAccountName = fromAcc.name,
                toAccountId = toAccountId,
                toAccountName = toAcc.name,
                amount = amount,
                fee = fee,
                notes = notes
            )
        )

        val jeId = db.accountingDao().insertJournalEntry(
            JournalEntryEntity(
                entryNumber = "JE-TR-${System.currentTimeMillis() % 100000}",
                transactionType = "TRANSFER",
                description = "Transferred GH₵ $amount from ${fromAcc.name} to ${toAcc.name} (Fee: GH₵ $fee)"
            )
        )

        val lines = mutableListOf<JournalLineEntity>()
        lines.add(
            JournalLineEntity(
                journalEntryId = jeId,
                accountCode = ChartOfAccounts.CODE_BANK,
                accountName = toAcc.name,
                debit = amount,
                credit = 0.0
            )
        )
        if (fee > 0) {
            lines.add(
                JournalLineEntity(
                    journalEntryId = jeId,
                    accountCode = ChartOfAccounts.CODE_TRANSFER_FEES,
                    accountName = ChartOfAccounts.NAME_TRANSFER_FEES,
                    debit = fee,
                    credit = 0.0
                )
            )
        }
        lines.add(
            JournalLineEntity(
                journalEntryId = jeId,
                accountCode = ChartOfAccounts.CODE_CASH,
                accountName = fromAcc.name,
                debit = 0.0,
                credit = amount + fee
            )
        )
        db.accountingDao().insertJournalLines(lines)

        db.auditLogDao().insertLog(
            AuditLogEntity(
                action = "ACCOUNT_TRANSFER",
                module = "ACCOUNTS",
                details = "Transferred GH₵ $amount from ${fromAcc.name} to ${toAcc.name}"
            )
        )
    }

    // --- Stock Adjustment (Damage, Expiry, Loss, Correction) ---
    suspend fun adjustStock(
        productId: Long,
        newQuantity: Int,
        reason: String,
        notes: String
    ) = withContext(Dispatchers.IO) {
        val product = db.productDao().getProductById(productId) ?: return@withContext
        val prevQty = product.quantity
        val diff = newQuantity - prevQty
        if (diff == 0) return@withContext

        db.productDao().updateProduct(product.copy(quantity = newQuantity))
        db.stockAdjustmentDao().insertAdjustment(
            StockAdjustmentEntity(
                productId = productId,
                productName = product.name,
                previousQuantity = prevQty,
                newQuantity = newQuantity,
                changeQuantity = diff,
                reason = reason,
                notes = notes
            )
        )

        // If stock reduced due to damage/loss, record in General Ledger
        if (diff < 0) {
            val lossCost = kotlin.math.abs(diff) * product.costPrice
            val jeId = db.accountingDao().insertJournalEntry(
                JournalEntryEntity(
                    entryNumber = "JE-ADJ-${System.currentTimeMillis() % 100000}",
                    transactionType = "ADJUSTMENT",
                    description = "Stock adjustment ($reason): ${kotlin.math.abs(diff)} units of ${product.name}"
                )
            )
            db.accountingDao().insertJournalLines(
                listOf(
                    JournalLineEntity(
                        journalEntryId = jeId,
                        accountCode = ChartOfAccounts.CODE_STOCK_LOSS,
                        accountName = ChartOfAccounts.NAME_STOCK_LOSS,
                        debit = lossCost,
                        credit = 0.0
                    ),
                    JournalLineEntity(
                        journalEntryId = jeId,
                        accountCode = ChartOfAccounts.CODE_INVENTORY,
                        accountName = ChartOfAccounts.NAME_INVENTORY,
                        debit = 0.0,
                        credit = lossCost
                    )
                )
            )
        }

        db.auditLogDao().insertLog(
            AuditLogEntity(
                action = "STOCK_ADJUSTMENT",
                module = "INVENTORY",
                details = "Adjusted ${product.name} from $prevQty to $newQuantity ($reason)"
            )
        )
    }

    // --- Product CRUD ---
    suspend fun saveProduct(product: ProductEntity): Long = withContext(Dispatchers.IO) {
        if (product.id == 0L) {
            val id = db.productDao().insertProduct(product)
            db.auditLogDao().insertLog(
                AuditLogEntity(action = "CREATE_PRODUCT", module = "INVENTORY", details = "Added product ${product.name}")
            )
            id
        } else {
            db.productDao().updateProduct(product)
            db.auditLogDao().insertLog(
                AuditLogEntity(action = "UPDATE_PRODUCT", module = "INVENTORY", details = "Updated product ${product.name}")
            )
            product.id
        }
    }

    suspend fun deleteProduct(product: ProductEntity) = withContext(Dispatchers.IO) {
        db.productDao().deleteProduct(product)
        db.auditLogDao().insertLog(
            AuditLogEntity(action = "DELETE_PRODUCT", module = "INVENTORY", details = "Deleted product ${product.name}")
        )
    }

    // --- Customer & Supplier CRUD ---
    suspend fun saveCustomer(customer: CustomerEntity): Long = withContext(Dispatchers.IO) {
        if (customer.id == 0L) db.customerDao().insertCustomer(customer) else {
            db.customerDao().updateCustomer(customer)
            customer.id
        }
    }

    suspend fun saveSupplier(supplier: SupplierEntity): Long = withContext(Dispatchers.IO) {
        if (supplier.id == 0L) db.supplierDao().insertSupplier(supplier) else {
            db.supplierDao().updateSupplier(supplier)
            supplier.id
        }
    }

    // --- Employee & Attendance CRUD ---
    suspend fun saveEmployee(employee: EmployeeEntity): Long = withContext(Dispatchers.IO) {
        if (employee.id == 0L) db.employeeDao().insertEmployee(employee) else {
            db.employeeDao().updateEmployee(employee)
            employee.id
        }
    }

    suspend fun logAttendance(attendance: AttendanceEntity): Long = withContext(Dispatchers.IO) {
        db.employeeDao().insertAttendance(attendance)
    }

    // --- Account CRUD ---
    suspend fun saveAccount(account: FinancialAccountEntity): Long = withContext(Dispatchers.IO) {
        if (account.id == 0L) db.financialAccountDao().insertAccount(account) else {
            db.financialAccountDao().updateAccount(account)
            account.id
        }
    }

    // --- Profile & E-Books ---
    suspend fun saveProfile(profile: BusinessProfileEntity) = withContext(Dispatchers.IO) {
        db.businessProfileDao().insertOrUpdateProfile(profile)
    }

    suspend fun updateEBook(ebook: EBookEntity) = withContext(Dispatchers.IO) {
        db.eBookDao().updateEBook(ebook)
    }

    suspend fun addCustomEBook(title: String, category: String, content: String, summary: String) = withContext(Dispatchers.IO) {
        db.eBookDao().insertEBook(
            EBookEntity(
                title = title,
                category = category,
                level = "Intermediate",
                author = "Admin User",
                readTimeMinutes = 10,
                summary = summary,
                contentMarkdown = content
            )
        )
    }

    // --- Initial Seed Generator (Populates realistic Ghana business demo data on first launch) ---
    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        val existingProfile = db.businessProfileDao().getProfileSync()
        if (existingProfile == null) {
            db.businessProfileDao().insertOrUpdateProfile(
                BusinessProfileEntity(
                    id = 1,
                    businessName = "OBITRUST VENTURES GHANA",
                    tagline = "Wholesale & Retail Enterprise",
                    phone = "+233 24 456 7890",
                    email = "sales@obitrust.com.gh",
                    address = "Plot 18, Commercial Street, Accra Central",
                    tinNumber = "C003498124X",
                    currencySymbol = "GH₵",
                    taxScheme = "VAT_FLAT_RATE",
                    customTaxRate = 4.0,
                    receiptFooter = "Thank you for shopping with OBITRUST. Verified Ghana Business.",
                    pinCode = "1234",
                    isPinRequired = false,
                    activeUserRole = "Admin"
                )
            )
        }

        // Seed E-Books if empty
        val existingEbooks = db.eBookDao().getEBookById(1)
        if (existingEbooks == null) {
            db.eBookDao().insertEBooks(EBookCurriculum.books)
        }

        // Check if accounts exist
        val accounts = db.financialAccountDao().getAllAccountsList()
        if (accounts.isEmpty()) {
            val cashAccId = db.financialAccountDao().insertAccount(
                FinancialAccountEntity(name = "Main Cash Drawer", accountType = "CASH", balance = 8450.0, provider = "Cash")
            )
            val gcbBankId = db.financialAccountDao().insertAccount(
                FinancialAccountEntity(name = "GCB Bank Current", accountType = "BANK", balance = 24500.0, accountNumber = "10123984501", provider = "GCB Bank")
            )
            val mtnMomoId = db.financialAccountDao().insertAccount(
                FinancialAccountEntity(name = "MTN MoMo Merchant", accountType = "MOBILE_MONEY", balance = 12850.0, accountNumber = "0244123456", provider = "MTN")
            )
            val telecelCashId = db.financialAccountDao().insertAccount(
                FinancialAccountEntity(name = "Telecel Cash Agent", accountType = "MOBILE_MONEY", balance = 4200.0, accountNumber = "0209876543", provider = "Telecel")
            )
            val atMoneyId = db.financialAccountDao().insertAccount(
                FinancialAccountEntity(name = "AT Money Vault", accountType = "MOBILE_MONEY", balance = 1950.0, accountNumber = "0261122334", provider = "AT")
            )

            // Seed Customers
            val custKwameId = db.customerDao().insertCustomer(
                CustomerEntity(name = "Kwame Mensah", phone = "0244556677", email = "kwame.mensah@gmail.com", address = "Osu, Accra", totalPurchases = 14200.0, outstandingDebt = 1850.0)
            )
            val custAbenaId = db.customerDao().insertCustomer(
                CustomerEntity(name = "Abena Osei", phone = "0208112233", email = "abena.osei@yahoo.com", address = "East Legon, Accra", totalPurchases = 8900.0, outstandingDebt = 0.0)
            )
            val custKofiId = db.customerDao().insertCustomer(
                CustomerEntity(name = "Kofi Boateng & Sons", phone = "0559988776", email = "boateng.kofi@gmail.com", address = "Adum, Kumasi", totalPurchases = 28600.0, outstandingDebt = 4200.0)
            )

            // Seed Suppliers
            val suppNestleId = db.supplierDao().insertSupplier(
                SupplierEntity(name = "Nestle Ghana Ltd", contactPerson = "Samuel Addo", phone = "0302123456", address = "Tema Industrial Area", outstandingBalance = 6500.0, paymentTermsDays = 30)
            )
            val suppWilmarId = db.supplierDao().insertSupplier(
                SupplierEntity(name = "Wilmar Africa (Frytol)", contactPerson = "Grace Amponsah", phone = "0302789101", address = "Tema Harbour Road", outstandingBalance = 0.0, paymentTermsDays = 14)
            )
            val suppFanMilkId = db.supplierDao().insertSupplier(
                SupplierEntity(name = "FanMilk Ghana PLC", contactPerson = "David Quaye", phone = "0302334455", address = "North Industrial Area, Accra", outstandingBalance = 3200.0, paymentTermsDays = 30)
            )

            // Seed Employees
            db.employeeDao().insertEmployees(
                listOf(
                    EmployeeEntity(name = "Kojo Antwi", role = "Manager", phone = "0241002003", email = "kojo@obitrust.com.gh", baseSalary = 3500.0, joinedDate = "2024-03-01"),
                    EmployeeEntity(name = "Esi Dadzie", role = "Cashier", phone = "0203004005", email = "esi@obitrust.com.gh", baseSalary = 1800.0, joinedDate = "2025-01-15"),
                    EmployeeEntity(name = "Yaw Frimpong", role = "Accountant", phone = "0556007008", email = "yaw@obitrust.com.gh", baseSalary = 3200.0, joinedDate = "2024-06-01"),
                    EmployeeEntity(name = "Akosua Badu", role = "Inventory Officer", phone = "0278009001", email = "akosua@obitrust.com.gh", baseSalary = 2000.0, joinedDate = "2025-04-10")
                )
            )

            // Seed Products
            val prodMiloId = db.productDao().insertProduct(
                ProductEntity(name = "Nestle Milo 400g Tin", category = "Beverages", barcode = "7613035889101", costPrice = 32.0, sellingPrice = 42.0, quantity = 45, minStockLevel = 10, supplierName = "Nestle Ghana Ltd")
            )
            val prodFrytolId = db.productDao().insertProduct(
                ProductEntity(name = "Frytol Cooking Oil 5L", category = "Groceries", barcode = "6001001234567", costPrice = 110.0, sellingPrice = 145.0, quantity = 28, minStockLevel = 8, supplierName = "Wilmar Africa")
            )
            val prodRiceId = db.productDao().insertProduct(
                ProductEntity(name = "Royal Aroma Jasmine Rice 25kg", category = "Groceries", barcode = "8901234567890", costPrice = 360.0, sellingPrice = 440.0, quantity = 16, minStockLevel = 5, supplierName = "Wilmar Africa")
            )
            val prodPeakMilkId = db.productDao().insertProduct(
                ProductEntity(name = "Peak Evaporated Milk 160g (Pack of 24)", category = "Beverages", barcode = "8712800102030", costPrice = 140.0, sellingPrice = 175.0, quantity = 3, minStockLevel = 8, supplierName = "FrieslandCampina") // Low stock!
            )
            val prodSugarId = db.productDao().insertProduct(
                ProductEntity(name = "St. Louis Cube Sugar 500g", category = "Groceries", barcode = "3012345678901", costPrice = 14.0, sellingPrice = 19.0, quantity = 0, minStockLevel = 15, supplierName = "Direct Importer") // Out of stock!
            )
            val prodBelAquaId = db.productDao().insertProduct(
                ProductEntity(name = "Bel-Aqua Mineral Water 500ml (Pack of 16)", category = "Beverages", barcode = "6031234567894", costPrice = 28.0, sellingPrice = 38.0, quantity = 60, minStockLevel = 15, supplierName = "Blow Chem Industries")
            )
            val prodKeySoapId = db.productDao().insertProduct(
                ProductEntity(name = "Key Soap Bar (Pack of 6)", category = "Household", barcode = "6001112223334", costPrice = 45.0, sellingPrice = 60.0, quantity = 32, minStockLevel = 10, supplierName = "Unilever Ghana")
            )

            // Seed Some Realistic Historical Sales
            val now = System.currentTimeMillis()
            val dayMillis = 86400000L

            // Sale 1: Today Cash Sale
            recordSale(
                SaleEntity(
                    saleNumber = "OB-S-202611-001",
                    customerId = custAbenaId,
                    customerName = "Abena Osei",
                    subtotal = 330.0,
                    discountAmount = 10.0,
                    taxRate = 4.0,
                    taxAmount = 12.8,
                    totalAmount = 332.8,
                    amountPaid = 332.8,
                    balanceDue = 0.0,
                    paymentMethod = "MTN_MOMO",
                    paymentAccountId = mtnMomoId,
                    cashierName = "Esi Dadzie",
                    createdAt = now - 3600000L
                ),
                listOf(
                    SaleItemEntity(saleId = 0, productId = prodMiloId, productName = "Nestle Milo 400g Tin", quantity = 2, unitCost = 32.0, unitPrice = 42.0, subtotal = 84.0),
                    SaleItemEntity(saleId = 0, productId = prodFrytolId, productName = "Frytol Cooking Oil 5L", quantity = 1, unitCost = 110.0, unitPrice = 145.0, subtotal = 145.0),
                    SaleItemEntity(saleId = 0, productId = prodBelAquaId, productName = "Bel-Aqua Mineral Water 500ml", quantity = 2, unitCost = 28.0, unitPrice = 38.0, subtotal = 76.0)
                )
            )

            // Sale 2: Credit Sale to Kwame
            recordSale(
                SaleEntity(
                    saleNumber = "OB-S-202611-002",
                    customerId = custKwameId,
                    customerName = "Kwame Mensah",
                    subtotal = 880.0,
                    discountAmount = 0.0,
                    taxRate = 4.0,
                    taxAmount = 35.2,
                    totalAmount = 915.2,
                    amountPaid = 400.0,
                    balanceDue = 515.2,
                    paymentMethod = "CREDIT",
                    paymentAccountId = cashAccId,
                    cashierName = "Esi Dadzie",
                    createdAt = now - 7200000L
                ),
                listOf(
                    SaleItemEntity(saleId = 0, productId = prodRiceId, productName = "Royal Aroma Jasmine Rice 25kg", quantity = 2, unitCost = 360.0, unitPrice = 440.0, subtotal = 880.0)
                )
            )

            // Seed Some Realistic Expenses
            recordExpense(
                ExpenseEntity(
                    title = "Shop Electricity Prepaid Units",
                    category = "Electricity",
                    amount = 450.0,
                    paymentAccountId = mtnMomoId,
                    paymentMethod = "MTN_MOMO",
                    createdAt = now - (dayMillis * 2)
                )
            )
            recordExpense(
                ExpenseEntity(
                    title = "Store Maintenance & Ceiling Repair",
                    category = "Maintenance",
                    amount = 320.0,
                    paymentAccountId = cashAccId,
                    paymentMethod = "CASH",
                    createdAt = now - (dayMillis * 4)
                )
            )
            recordExpense(
                ExpenseEntity(
                    title = "Shop Rent Advance Monthly Share",
                    category = "Rent",
                    amount = 1200.0,
                    paymentAccountId = gcbBankId,
                    paymentMethod = "BANK_TRANSFER",
                    createdAt = now - (dayMillis * 6)
                )
            )

            db.auditLogDao().insertLog(
                AuditLogEntity(action = "SYSTEM_INIT", module = "SYSTEM", details = "Initialized Obitrust Business Manager with realistic demo business records.")
            )
        }
    }
}
