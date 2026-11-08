package com.example.obitrust.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val category: String = "General Goods",
    val barcode: String = "",
    val costPrice: Double,
    val sellingPrice: Double,
    val quantity: Int,
    val minStockLevel: Int = 5,
    val supplierName: String = "",
    val storageLocation: String = "Main Store",
    val expiryDate: String = "",
    val imageUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleNumber: String,
    val customerId: Long? = null,
    val customerName: String = "Walk-in Customer",
    val subtotal: Double,
    val discountAmount: Double = 0.0,
    val taxRate: Double = 0.0,
    val taxAmount: Double = 0.0,
    val totalAmount: Double,
    val amountPaid: Double,
    val balanceDue: Double = 0.0,
    val paymentMethod: String = "CASH", // CASH, BANK_TRANSFER, MTN_MOMO, TELECEL_CASH, AT_MONEY, CREDIT
    val paymentAccountId: Long? = null,
    val status: String = "COMPLETED", // COMPLETED, VOIDED
    val notes: String = "",
    val cashierName: String = "Admin",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "sale_items",
    indices = [Index("saleId"), Index("productId")]
)
data class SaleItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val unitCost: Double,
    val unitPrice: Double,
    val subtotal: Double
)

@Entity(tableName = "purchases")
data class PurchaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val purchaseNumber: String,
    val supplierId: Long? = null,
    val supplierName: String,
    val invoiceNumber: String = "",
    val totalAmount: Double,
    val amountPaid: Double,
    val balanceDue: Double = 0.0,
    val paymentMethod: String = "CASH",
    val paymentAccountId: Long? = null,
    val status: String = "RECEIVED", // RECEIVED, PENDING, CANCELLED
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "purchase_items",
    indices = [Index("purchaseId"), Index("productId")]
)
data class PurchaseItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val purchaseId: Long,
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val unitCost: Double,
    val subtotal: Double
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String, // Rent, Electricity, Water, Transportation, Salaries, Fuel, Internet, Maintenance, Food, Stationery, Marketing, Miscellaneous
    val amount: Double,
    val paymentAccountId: Long? = null,
    val paymentMethod: String = "CASH",
    val isRecurring: Boolean = false,
    val receiptUri: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val email: String = "",
    val address: String = "",
    val totalPurchases: Double = 0.0,
    val outstandingDebt: Double = 0.0,
    val creditLimit: Double = 5000.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val contactPerson: String = "",
    val phone: String,
    val email: String = "",
    val address: String = "",
    val totalPurchases: Double = 0.0,
    val outstandingBalance: Double = 0.0,
    val paymentTermsDays: Int = 30,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "employees")
data class EmployeeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val role: String = "Cashier", // Admin, Manager, Cashier, Accountant, Inventory Officer
    val phone: String,
    val email: String = "",
    val baseSalary: Double = 0.0,
    val commissionRate: Double = 0.0,
    val status: String = "ACTIVE", // ACTIVE, ON_LEAVE, TERMINATED
    val joinedDate: String = "2026-01-01"
)

@Entity(tableName = "attendance")
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val employeeId: Long,
    val employeeName: String,
    val dateMillis: Long = System.currentTimeMillis(),
    val status: String = "PRESENT", // PRESENT, ABSENT, LATE, HALF_DAY
    val notes: String = ""
)

@Entity(tableName = "financial_accounts")
data class FinancialAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val accountType: String, // CASH, BANK, MOBILE_MONEY, OTHER
    val balance: Double = 0.0,
    val accountNumber: String = "",
    val provider: String = "", // Cash, MTN, Telecel, AT, GCB, Ecobank
    val notes: String = ""
)

@Entity(tableName = "account_transfers")
data class AccountTransferEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fromAccountId: Long,
    val fromAccountName: String,
    val toAccountId: Long,
    val toAccountName: String,
    val amount: Double,
    val fee: Double = 0.0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "journal_entries")
data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entryNumber: String,
    val transactionType: String, // SALE, PURCHASE, EXPENSE, CUSTOMER_PAYMENT, SUPPLIER_PAYMENT, TRANSFER, SALARY, ADJUSTMENT, CAPITAL, DRAWINGS
    val referenceId: Long? = null,
    val referenceCode: String = "",
    val description: String,
    val dateMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "journal_lines",
    indices = [Index("journalEntryId")]
)
data class JournalLineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val journalEntryId: Long,
    val accountCode: String,
    val accountName: String,
    val debit: Double = 0.0,
    val credit: Double = 0.0
)

@Entity(tableName = "stock_adjustments")
data class StockAdjustmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val productName: String,
    val previousQuantity: Int,
    val newQuantity: Int,
    val changeQuantity: Int,
    val reason: String, // DAMAGED, LOST/MISSING, EXPIRED, AUDIT_CORRECTION, RETURN, BRANCH_TRANSFER
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val action: String,
    val module: String,
    val details: String,
    val user: String = "Admin",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "ebooks")
data class EBookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String,
    val level: String = "Beginner", // Beginner, Intermediate, Advanced
    val author: String = "Obitrust Business Academy",
    val readTimeMinutes: Int = 15,
    val summary: String,
    val contentMarkdown: String,
    val isBookmarked: Boolean = false,
    val progressPercent: Int = 0,
    val userNotes: String = ""
)

@Entity(tableName = "business_profile")
data class BusinessProfileEntity(
    @PrimaryKey val id: Long = 1,
    val businessName: String = "OBITRUST VENTURES GHANA",
    val tagline: String = "Integrity, Excellence & Reliable Growth",
    val phone: String = "+233 24 123 4567",
    val email: String = "info@obitrust.com.gh",
    val address: String = "Plot 14, Commercial Avenue, Accra Central, Ghana",
    val tinNumber: String = "C0012345678",
    val currencySymbol: String = "GH₵",
    val taxScheme: String = "VAT_STANDARD", // VAT_STANDARD (21.9%), VAT_FLAT_RATE (4%), NONE, CUSTOM
    val customTaxRate: Double = 21.9,
    val receiptFooter: String = "Thank you for doing business with OBITRUST. Items sold in good condition are not returnable after 48 hours.",
    val pinCode: String = "1234",
    val isPinRequired: Boolean = false,
    val activeUserRole: String = "Admin",
    val logoUri: String = "",
    val selectedLogoPreset: String = "SHIELD"
)
